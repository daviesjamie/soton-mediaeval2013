package org.openimaj.mediaeval.searchhyper2013.tools;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Result;
import org.openimaj.mediaeval.searchhyper2013.searcher.EpsilonSearcher;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherEvaluator;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;
import org.openimaj.mediaeval.searchhyper2013.util.LSHDataExplorer;
import org.xml.sax.SAXException;

public class EpsilonSearcherTool {
	public static final int WINDOW = 300;

	public static void main(String[] args) throws IOException, SearcherException, ParserConfigurationException, SAXException {
		/*if (args[0].equalsIgnoreCase("Search")) {
			search(new File(args[1]), args[2], new File(args[3]), new File(args[4]), new File(args[5]), new File(args[6]));
		} else*/ if (args[0].equalsIgnoreCase("Evaluate")) {
			evaluate(new File(args[1]), new File(args[2]), new File(args[3]), new File(args[4]), new File(args[5]), new File(args[6]), new File(args[7]), new File(args[8]));
		} else {
			System.err.println("Unrecognised mode. Recognised modes are " + 
							   "'Search' and 'Evaluate'.");
		}
	}
	
	/*private static void search(File index, String query, File shotsDirCacheFile, File graphFile, File conceptsDir, File conceptsFile, File synFile) throws IOException, SearcherException {
		Directory indexDir = FSDirectory.open(index);
		IndexReader indexReader = DirectoryReader.open(indexDir);
		
		LSHDataExplorer lshExplorer = new LSHDataExplorer(graphFile, 10);
		
		EpsilonSearcher epsilonSearcher = new EpsilonSearcher("EpsilonSearcher", indexReader, shotsDirCacheFile, lshExplorer, conceptsDir, conceptsFile, synFile);
		
		Query q = new Query("CLI", query, null);
		
		System.out.println(epsilonSearcher.search(q));
	}*/
	
	private static void evaluate(File index,
								 File queriesFile,
								 File resultsFile,
								 File shotsDirCacheFile,
								 File graphFile,
								 File conceptsDir,
								 File conceptsFile,
								 File synFile) 
										 throws IOException,
										 		ParserConfigurationException,
										 		SAXException {
		System.out.println("Opening index...");
		Directory indexDir = FSDirectory.open(index);
		IndexReader indexReader = DirectoryReader.open(indexDir);
		
		LSHDataExplorer lshExplorer = new LSHDataExplorer(graphFile, 10);
		
		EpsilonSearcher epsilonSearcher = new EpsilonSearcher("EpsilonSearcher", indexReader, shotsDirCacheFile, lshExplorer, conceptsDir, conceptsFile, synFile);
		
		SearcherEvaluator eval = new SearcherEvaluator(epsilonSearcher);
		
		Map<Query, List<Result>> expectedResults = 
				SearcherEvaluator.importExpected(queriesFile, resultsFile);
		
		Float[] initial     = { 0f, 1.5f, 2f, 1f, 1f, 0f, 0f, 60 * 1f, 60 * 10f, 0.5f, 0.3f, 2f,   0f,   0f };
		Float[] increment   = { 1f,   1f, 1f, 1f, 1f, 1f, 1f,      1f,       1f,   1f,   1f, 1f, 0.1f,   1f };
		Float[] termination = { 0f, 1.5f, 2f, 1f, 1f, 0f, 0f, 60 * 1f, 60 * 10f, 0.5f, 0.3f, 2f,   3f, 100f };
		
		List<Float[]> evaluation = eval.evaluateOverSettings(expectedResults,
															 WINDOW,
															 initial, 
															 increment, 
															 termination);
		/*Vector evaluation =
				eval.evaluateAgainstExpectedResults(expectedResults, WINDOW);
		
		System.out.println(evaluation);
		System.out.println(SearcherEvaluator.f1Score(evaluation));*/
	}

}
