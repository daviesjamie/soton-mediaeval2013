package org.openimaj.mediaeval.searchhyper2013.searcher;

import gov.sandia.cognition.math.matrix.Vector;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineFactory;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineSet;
import org.openimaj.mediaeval.searchhyper2013.lucene.EnglishSynonymAnalyzer;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.LuceneUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.TimeStringFormatter;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.ChannelFilterModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.ConceptModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.JustifiedTimedFunction;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.LSHGraphModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.SearcherModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.SynopsisModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.TitleModule;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.TranscriptModule;
import org.openimaj.mediaeval.searchhyper2013.util.LSHDataExplorer;
import org.openimaj.mediaeval.searchhyper2013.util.SlidingWindowUnivariateSolver;
import org.openimaj.mediaeval.searchhyper2013.util.Time;
import org.xml.sax.SAXException;

import de.jungblut.clustering.MeanShiftClustering;
import de.jungblut.math.DoubleVector;

public class ModularSearcher implements Searcher {

	double SOLUTION_WINDOW = 1 * 60;
	double MERGE_WINDOW = 5 * 60;
	double SCORE_WINDOW = 5 * 60;
	
	int SHOTS_WIDTH = 10;
	int MAX_RESULTS_PER_TIMELINE = 10;

	String runName;
	UnivariateFunction resultWindow;
	
	StandardQueryParser queryParser;
	List<SearcherModule> searcherModules;
	
	TimelineFactory timelineFactory;
	
	public ModularSearcher(String runName,
						   UnivariateFunction resultWindow) {
		this.runName = runName;
		this.resultWindow = resultWindow;
		
		queryParser = new StandardQueryParser();
		searcherModules = new ArrayList<SearcherModule>();
		
		this.timelineFactory = timelineFactory;
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
		Query query = cleanQuery(q);
		
		// Accumulated timelines.
		TimelineSet timelines = new TimelineSet();
		
		for (SearcherModule module : searcherModules) {
			timelines = module.search(query, timelines);
		}
		
		//System.out.println("No. timelines: " + timelines.size());
		
		//ResultList results = kMeans(q, timelines);
		ResultList results = shotBoundaries(q, timelines);
		
		Collections.sort(results);
		
		return results;
	}
	
	private ResultList shotBoundaries(Query q, TimelineSet timelines) throws Exception {
		UnivariateIntegrator integrator = 
				new TrapezoidIntegrator(1e-3, 1e-3, 2, 64);
		
		ResultSet resultSet = new ResultSet();
		
		for (Timeline timeline : timelines) {
			System.out.println(timeline);
			
			List<JustifiedTimedFunction> fs =
				new ArrayList<JustifiedTimedFunction>(timeline.getFunctions());
			Collections.sort(fs, new JustifiedTimedFunction.TimeComparator());
			
			for (JustifiedTimedFunction f : fs) {
				System.out.println("\t" + f.toString());
				
				for (String j : f.getJustifications()) {
					System.out.println("\t\t" + j);
				}
			}
			
			float[] shotBoundaries = timeline.getShotBoundaries();
			
			double[] integrals = new double[shotBoundaries.length];
			
			for (int i = 0; i < shotBoundaries.length; i++) {
				float start = i == 0 ? 0 : shotBoundaries[i - 1];
				float end = shotBoundaries[i];
				
				integrals[i] = integrator.integrate(10000,
													timeline,
													start,
													end);
			}
			
			double[] bestIntegrals = Arrays.copyOf(integrals, integrals.length);
			Arrays.sort(bestIntegrals);
			bestIntegrals =
					Arrays.copyOfRange(bestIntegrals,
								   	   Math.max(bestIntegrals.length -
								   			   		MAX_RESULTS_PER_TIMELINE,
								   			   	0),
								   	   bestIntegrals.length);
			
			for (double integral : bestIntegrals) {
				int startIndex = -2;
				
				for (int i = 0; i < integrals.length; i++) {
					if (integrals[i] == integral) {
						startIndex = i - 1;
						
						break;
					}
				}
				
				if (startIndex == -2) {
					throw new Exception("OSHIT");
				}
				
				int minBoundaryIndex = startIndex - SHOTS_WIDTH;
				int maxBoundaryIndex = startIndex + 1 + SHOTS_WIDTH;
				
				float start = minBoundaryIndex < 0 ?
								0 : shotBoundaries[minBoundaryIndex];
				float jumpIn = startIndex < 0 ? 0 : shotBoundaries[startIndex];
				float end = maxBoundaryIndex > shotBoundaries.length - 1 ?
								timeline.getEndTime() : 
								shotBoundaries[maxBoundaryIndex];
								
				double score = integrator.integrate(10000,
													timeline,
													start,
													end);
				
				Result result = new Result();
				
				result.fileName = timeline.getID();
				result.startTime = start;
				result.jumpInPoint = jumpIn;
				result.endTime = end;
				result.confidenceScore = score;
				
				resultSet.add(result);
			}
		}
		
		// Normalise.
		double maxScore = 0;
		
		for (Result result : resultSet) {
			maxScore = Math.max(result.confidenceScore, maxScore);
		}
		
		for (Result result : resultSet) {
			result.confidenceScore /= maxScore;
		}
		
		ResultList results = new ResultList(q.queryID, runName);
		
		results.addAll(resultSet);
		
		return results;
	}
	
	private ResultList kMeans(Query q, TimelineSet timelines) {
		UnivariateIntegrator integrator = 
				new TrapezoidIntegrator(1e-3, 1e-3, 2, 64);
		
		ResultSet resultSet = new ResultSet();
		
		for (Timeline timeline : timelines) {
			System.out.println(timeline);
			
			List<JustifiedTimedFunction> fs =
				new ArrayList<JustifiedTimedFunction>(timeline.getFunctions());
			Collections.sort(fs, new JustifiedTimedFunction.TimeComparator());
			
			for (JustifiedTimedFunction f : fs) {
				System.out.println("\t" + f.toString());
				
				for (String j : f.getJustifications()) {
					System.out.println("\t\t" + j);
				}
			}
			
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
					(float) Math.min(timeline.getEndTime(),
						 		 	 solutionTime + resultWindow.value(score));
				result.confidenceScore = score;
				
				resultSet.add(result);
			}
		}

		ResultList results = new ResultList(q.queryID, runName);
		
		results.addAll(resultSet);
		
		return results;
	}
	
	public Query cleanQuery(Query q) {
		String[] stopwords = { "from" };
		
		Query newQ = new Query(q);
		
		for (String word : stopwords) {
			Pattern pattern = Pattern.compile(word, Pattern.CASE_INSENSITIVE);
			
			newQ.queryText = pattern.matcher(newQ.queryText).replaceAll("");
			newQ.visualCues = pattern.matcher(newQ.visualCues).replaceAll("");
		}
		
		return newQ;
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
		
		TimelineFactory timelineFactory = 
				new TimelineFactory(indexSearcher, new File(args[3]));
		
		//LSHDataExplorer lshGraph = new LSHDataExplorer(new File(args[4]), 20);
		
		ChannelFilterModule channelFilterModule = new ChannelFilterModule();
		SynopsisModule synopsisModule = new SynopsisModule(indexSearcher,
														   timelineFactory);
		TitleModule titleModule = new TitleModule(indexSearcher,
												  timelineFactory);
		TranscriptModule transcriptModule =
				new TranscriptModule(indexSearcher,
									 Type.Subtitles,
									 englishAnalyzer,
									 timelineFactory);
		ConceptModule conceptModule = new ConceptModule(new File(args[5]),
														new File(args[6]),
														englishAnalyzer);
		//LSHGraphModule lshGraphModule = new LSHGraphModule(new File(args[7]),
		//												   lshGraph,
		//												   timelineFactory);
		
		ModularSearcher searcher = new ModularSearcher("ModularSearcher",
													   resultWindow);
		searcher.addModule(synopsisModule);
		searcher.addModule(titleModule);
		searcher.addModule(transcriptModule);
		searcher.addModule(channelFilterModule);
		searcher.addModule(conceptModule);
		//searcher.addModule(lshGraphModule);
		
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
		
		System.out.println(results);
	}
}
