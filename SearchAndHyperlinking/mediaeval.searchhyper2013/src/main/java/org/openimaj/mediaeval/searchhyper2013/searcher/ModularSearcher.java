package org.openimaj.mediaeval.searchhyper2013.searcher;

import gov.sandia.cognition.math.matrix.Vector;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.function.Constant;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
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
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.LuceneUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.TimeStringFormatter;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;
import org.openimaj.mediaeval.searchhyper2013.util.SlidingWindowUnivariateSolver;
import org.xml.sax.SAXException;

public class ModularSearcher implements Searcher {
	
	Version LUCENE_VERSION = Version.LUCENE_43;
	
	double SYNOPSIS_WEIGHT = 1;
	double SYNOPSIS_POWER = 2;
	
	double TRANSCRIPT_WEIGHT = 1;
	double TRANSCRIPT_POWER = 2;
	double TRANSCRIPT_WIDTH = 60;
	Type TRANSCRIPT_TYPE = Type.Subtitles;
	
	double SOLUTION_WINDOW = 5 * 60;
	double SCORE_WINDOW = 5 * 60;
	
	String runName;
	IndexSearcher indexSearcher;
	UnivariateFunction resultWindow;
	
	StandardQueryParser queryParser;
	
	public ModularSearcher(String runName,
						   IndexSearcher indexSearcher,
						   UnivariateFunction resultWindow) {
		this.runName = runName;
		this.indexSearcher = indexSearcher;
		this.resultWindow = resultWindow;
		
		queryParser = new StandardQueryParser();
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
		
		if (SYNOPSIS_WEIGHT > 0) {
			timelines.addAll(getSynopsisTimelines(q));
		}
		
		if (TRANSCRIPT_WEIGHT > 0) {
			timelines.addAll(getTranscriptTimelines(q));
		}
		
		System.out.println(timelines);
		
		FiniteDifferencesDifferentiator differentiator = 
				new FiniteDifferencesDifferentiator(4, TRANSCRIPT_WIDTH / 8);
		SlidingWindowUnivariateSolver<UnivariateDifferentiableFunction> solver = 
			new SlidingWindowUnivariateSolver<UnivariateDifferentiableFunction>(
				new NewtonRaphsonSolver());
		
		RombergIntegrator integrator = new RombergIntegrator();
		
		ResultSet resultSet = new ResultSet();
		
		for (Timeline timeline : timelines) {
			ChartFrame chart = new ChartFrame(timeline.getID(), timeline.plot());
			chart.setVisible(true);
			
			UnivariateDifferentiableFunction differentiatedTimeline =
					differentiator.differentiate(timeline);
			UnivariateDifferentiableFunction doublyDifferentiatedTimeline =
					differentiator.differentiate(differentiatedTimeline);
			
			double[] solutions = solver.solve(1000,
											  differentiatedTimeline,
											  0,
											  timeline.endTime,
											  SOLUTION_WINDOW,
											  SOLUTION_WINDOW);
			
			for (double solutionTime : solutions) {	
				if (doublyDifferentiatedTimeline.value(solutionTime) < 0) {
					double score =
							integrator.integrate(1000,
												 timeline,
												 solutionTime - SCORE_WINDOW,
												 solutionTime + SCORE_WINDOW);
					
					Result result = new Result();
					
					result.fileName = timeline.getID();
					result.startTime =
							(float) (solutionTime - resultWindow.value(score));
					result.jumpInPoint = result.startTime;
					result.endTime = 
							(float) (solutionTime + resultWindow.value(score));
					result.confidenceScore = (float) score;
					
					resultSet.add(result);
				}
			}
		}
		
		ResultList results = new ResultList(q.queryID, runName);
		
		results.addAll(resultSet);
		
		return results;
	}
	
	private TimelineSet getSynopsisTimelines(Query q) throws IOException, QueryNodeException {
		org.apache.lucene.search.Query luceneQuery = 
				queryParser.parse(q.queryText, Field.Text.toString());
		Filter synopsisFilter = new QueryWrapperFilter(
									new TermQuery(
										new Term(Field.Type.toString(),
												 Type.Synopsis.toString())));
		
		ScoreDoc[] hits = LuceneUtils.normaliseTopDocs(
							indexSearcher.search(luceneQuery,
												 synopsisFilter,
												 1000000));
		
		TimelineSet timelines = new TimelineSet();
		
		for (ScoreDoc doc : hits) {
			Document luceneDocument = indexSearcher.doc(doc.doc);
			
			Timeline programmeTimeline =
				new Timeline(luceneDocument.get(Field.Program.toString()),
							 Float.parseFloat(
								luceneDocument.get(Field.Length.toString())));
			programmeTimeline.addFunction(
					new Constant(SYNOPSIS_WEIGHT *
									Math.pow(doc.score,
											 SYNOPSIS_POWER)));
			
			timelines.add(programmeTimeline);
		}
		
		return timelines;
	}
	
	private TimelineSet getTranscriptTimelines(Query q) throws IOException, InvalidTokenOffsetsException, QueryNodeException {
		org.apache.lucene.search.Query luceneQuery = 
				queryParser.parse(q.queryText, Field.Text.toString());
		Filter transcriptFilter = new QueryWrapperFilter(
									new TermQuery(
										new Term(Field.Type.toString(),
												 TRANSCRIPT_TYPE.toString())));
		
		ScoreDoc[] hits = LuceneUtils.normaliseTopDocs(
							indexSearcher.search(luceneQuery,
												 transcriptFilter,
												 1000000));
		
		TimelineSet timelines = new TimelineSet();
		
		for (ScoreDoc doc : hits) {
			Document luceneDocument = indexSearcher.doc(doc.doc);
			
			Document synopsis =
					LuceneUtils.resolveOtherFromProgramme(doc.doc,
														  Type.Synopsis,
														  indexSearcher);
			
			Timeline programmeTimeline =
				new Timeline(luceneDocument.get(Field.Program.toString()),
							 Float.parseFloat(
									 synopsis.get(Field.Length.toString())));
			
			String transcript = luceneDocument.get(Field.Text.toString());
			
			Highlighter timeHighlighter = 
					new Highlighter(
						new TimeStringFormatter(
							transcript,
							luceneDocument.get(Field.Times.toString())),
						new DefaultEncoder(),
						new QueryTermScorer(luceneQuery));
			TokenStream tokenStream =
					new EnglishAnalyzer(LUCENE_VERSION)
							.tokenStream(Field.Text.toString(),
										 new StringReader(transcript));
			
			String timeString = 
					timeHighlighter.getBestFragments(tokenStream,
													 transcript,
													 1000000, "");
			
			tokenStream.close();
			
			List<Float> times = TimeStringFormatter.timesFromString(timeString);
			
			for (float time : times) {
				programmeTimeline.addFunction(
						new Gaussian(TRANSCRIPT_WEIGHT *
									 	Math.pow(doc.score,
									 			 TRANSCRIPT_POWER),
									 time,
									 TRANSCRIPT_WIDTH / 3d));
			}
			
			timelines.add(programmeTimeline);
		}
		
		return timelines;
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
				return 60 * x;
			}
			
		};
		
		ModularSearcher searcher = new ModularSearcher("ModularSearcher",
													   indexSearcher,
													   resultWindow);
		
		SearcherEvaluator evaluator = new SearcherEvaluator(searcher);
		
		Map<Query, List<Result>> expectedResults = 
				SearcherEvaluator.importExpected(queriesFile, resultsFile);
		
		Vector results =
				evaluator.evaluateAgainstExpectedResults(expectedResults,
														 60 * 5);
	}
}
