package org.openimaj.mediaeval.searchhyper2013.searcher;

import gov.sandia.cognition.math.matrix.Vector;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.function.Constant;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.analysis.solvers.NewtonRaphsonSolver;
import org.apache.commons.math3.analysis.solvers.PegasusSolver;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryTermScorer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jfree.chart.ChartFrame;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Result;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ResultList;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ResultSet;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Timeline;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineSet;
import org.openimaj.mediaeval.searchhyper2013.lucene.EnglishSynonymAnalyzer;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.LuceneUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.TimeStringFormatter;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.ChannelFilterModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.ConceptModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.SearcherModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.SynopsisModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.TitleModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.TranscriptModule;
import org.openimaj.mediaeval.searchhyper2013.util.SlidingWindowUnivariateSolver;
import org.openimaj.mediaeval.searchhyper2013.util.Time;
import org.xml.sax.SAXException;

import de.jungblut.clustering.MeanShiftClustering;
import de.jungblut.math.DoubleVector;

public class ModularSearcher implements Searcher {

	double SOLUTION_WINDOW = 1 * 60;
	double MERGE_WINDOW = 5 * 60;
	double SCORE_WINDOW = 5 * 60;

	String runName;
	UnivariateFunction resultWindow;
	
	StandardQueryParser queryParser;
	List<SearcherModule> searcherModules;
	
	public ModularSearcher(String runName,
						   UnivariateFunction resultWindow) {
		this.runName = runName;
		this.resultWindow = resultWindow;
		
		queryParser = new StandardQueryParser();
		searcherModules = new ArrayList<SearcherModule>();
	}
	
	@Override
	public ResultList search(Query q) throws SearcherException {
		try {
			return _search(q);
		} catch (Exception e) {
			throw new SearcherException(e);
		}
	}
	
	public ResultList _search(Query q) throws Exception {
		// Accumulated timelines.
		TimelineSet timelines = new TimelineSet();
		
		for (SearcherModule module : searcherModules) {
			timelines = module.search(q, timelines);
		}
		
		//System.out.println("No. timelines: " + timelines.size());
		
		UnivariateIntegrator integrator = 
				new TrapezoidIntegrator(1e-3, 1e-3, 2, 64);
		
		ResultSet resultSet = new ResultSet();
		
		for (Timeline timeline : timelines) {
			//ChartFrame chartFrame = new ChartFrame(timeline.getID(),
			//									   timeline.plot());
			//chartFrame.setVisible(true);
			
			//System.out.println(timeline);
			
			List<DoubleVector> sample = timeline.sample();
			
			//System.out.println("No. samples: " + sample.size());
			
			List<DoubleVector> clusters =
					MeanShiftClustering.cluster(sample,
												SOLUTION_WINDOW,
												MERGE_WINDOW,
												1000,
												false);
			
			//System.out.println("No. clusters: " + clusters.size());
			
			Map<Float, Double> clusterScores =
					new HashMap<Float, Double>();
			
			double maxScore = 0;
			
			for (DoubleVector cluster : clusters) {
				float solutionTime = (float) cluster.get(0);
				
				double score =
							integrator.integrate((int) 1e4,
											 	 timeline,
											 	 solutionTime - SCORE_WINDOW,
											 	 solutionTime + SCORE_WINDOW);
				
				clusterScores.put(solutionTime, score);
				
				maxScore = Math.max(maxScore, score);
			}
			
			// Normalise scores.
			for (Float solutionTime : clusterScores.keySet()) {
				clusterScores.put(solutionTime,
								  clusterScores.get(solutionTime) / maxScore);
			}
			
			for (Float solutionTime : clusterScores.keySet()) {
				double score = clusterScores.get(solutionTime);
				
				Result result = new Result();
				
				result.fileName = timeline.getID();
				result.startTime =
					(float) Math.max(0,
							 		 solutionTime - resultWindow.value(score));
				result.jumpInPoint = result.startTime;
				result.endTime = 
					(float) Math.min(timeline.endTime,
						 		 	 solutionTime + resultWindow.value(score));
				result.confidenceScore = score;
				
				resultSet.add(result);
			}
		}

		ResultList results = new ResultList(q.queryID, runName);
		
		results.addAll(resultSet);
		
		Collections.sort(results);
		
		return results;
	}

	public boolean addModule(SearcherModule module) {
		return searcherModules.add(module);
	}
	
	@Override
	public void configure(Float[] settings) {
		// TODO Auto-generated method stub

	}

	@Override
	public int numSettings() {
		// TODO Auto-generated method stub
		return 0;
	}

	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
		File queriesFile = new File(args[0]);
		File resultsFile = new File(args[1]);
		
		Directory luceneDir = FSDirectory.open(new File(args[2]));
		IndexReader indexReader = DirectoryReader.open(luceneDir);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		
		UnivariateFunction resultWindow = new UnivariateFunction() {

			@Override
			public double value(double x) {
				return 2 * 60 * x;
			}
			
		};

		EnglishAnalyzer englishAnalyzer =
				new EnglishAnalyzer(Version.LUCENE_43);
		
		ChannelFilterModule channelFilterModule = new ChannelFilterModule();
		SynopsisModule synopsisModule = new SynopsisModule(indexSearcher);
		TitleModule titleModule = new TitleModule(indexSearcher);
		TranscriptModule transcriptModule =
				new TranscriptModule(indexSearcher,
									 Type.LIMSI,
									 englishAnalyzer);
		ConceptModule conceptModule = new ConceptModule(new File(args[3]),
														new File(args[4]),
														englishAnalyzer);
		
		ModularSearcher searcher = new ModularSearcher("ModularSearcher",
													   resultWindow);
		searcher.addModule(synopsisModule);
		searcher.addModule(titleModule);
		searcher.addModule(transcriptModule);
		searcher.addModule(channelFilterModule);
		searcher.addModule(conceptModule);
		
		// Filter for synopsis hits.
		searcher.addModule(new SearcherModule() {

			@Override
			public TimelineSet search(Query q, TimelineSet currentSet)
					throws SearcherException {
				TimelineSet timelines = new TimelineSet();
				
				for (Timeline timeline : currentSet) {
					if (!(timeline.containsInstanceOf(
							SynopsisModule.SynopsisFunction.class) &&
						timeline.numFunctions() == 1)) {
							timelines.add(timeline);
					}
				}
				
				return timelines;
			}
			
		});
		
		SearcherEvaluator evaluator = new SearcherEvaluator(searcher);
		
		Map<Query, List<Result>> expectedResults = 
				SearcherEvaluator.importExpected(queriesFile, resultsFile);
		
		Vector results =
				evaluator.evaluateAgainstExpectedResults(expectedResults,
														 60 * 5);
	}
}
