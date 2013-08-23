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
import org.openimaj.mediaeval.searchhyper2013.searcher.BetaSearcher;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherEvaluator;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;
import org.xml.sax.SAXException;

public class BetaSearcherTool {
	public static final int WINDOW = 300;

	public static void main(String[] args) throws IOException, SearcherException, ParserConfigurationException, SAXException {
		if (args[0].equalsIgnoreCase("Search")) {
			search(new File(args[1]), args[2]);
		} else if (args[0].equalsIgnoreCase("Evaluate")) {
			evaluate(new File(args[1]), new File(args[2]), new File(args[3]));
		} else {
			System.err.println("Unrecognised mode. Recognised modes are " + 
							   "'Search' and 'Evaluate'.");
		}
	}
	
	private static void search(File index, String query) throws IOException, SearcherException {
		Directory indexDir = FSDirectory.open(index);
		
		IndexReader indexReader = DirectoryReader.open(indexDir);
		
		BetaSearcher betaSearcher = new BetaSearcher("BetaSearcher", indexReader);
		
		Query q = new Query("CLI", query, null);
		
		System.out.println(betaSearcher.search(q));
	}
	
	private static void evaluate(File index, File queriesFile, File resultsFile) throws IOException, ParserConfigurationException, SAXException {
		Directory indexDir = FSDirectory.open(index);
		
		IndexReader indexReader = DirectoryReader.open(indexDir);
		
		BetaSearcher betaSearcher = new BetaSearcher("BetaSearcher", indexReader);
		
		SearcherEvaluator eval = new SearcherEvaluator(betaSearcher);
		
		Map<Query, List<Result>> expectedResults = 
				SearcherEvaluator.importExpected(queriesFile, resultsFile);
		
		Float[] initial = { 1f, 0f, 0f, 60 * 1f, 60 * 10f, 0f };
		Float[] increment = { 1f, 1f, 1f, 1f, 1f, 0.1f };
		Float[] termination = { 1f, 0f, 0f, 60 * 1f, 60 * 10f, 3f };
		
		List<Float[]> evaluation = eval.evaluateOverSettings(expectedResults,
															 WINDOW,
															 initial, 
															 increment, 
															 termination);
		
		//System.out.println(listToCSV(evaluation));
	}
	
	public static String listToCSV(List<Float[]> list) {
		StringBuilder string = new StringBuilder();
		
		for (Object[] line : list) {
			for (Object item : line) {
				string.append(item.toString() + ", ");
			}
			
			string.replace(string.length() - 2, string.length(), "");
			
			string.append("\n");
		}
		
		return string.toString();
	}
}