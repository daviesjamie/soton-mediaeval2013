package org.openimaj.mediaeval.searchhyper2013;

import gov.sandia.cognition.math.matrix.Vector;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.xml.sax.SAXException;

public class AlphaSearcherTool {
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
		
		AlphaSearcher alphaSearcher = new AlphaSearcher("AlphaSearcher", indexReader);
		
		Query q = new Query("CLI", query, null);
		
		System.out.println(alphaSearcher.search(q));
	}
	
	private static void evaluate(File index, File queriesFile, File resultsFile) throws IOException, ParserConfigurationException, SAXException {
		Directory indexDir = FSDirectory.open(index);
		
		IndexReader indexReader = DirectoryReader.open(indexDir);
		
		AlphaSearcher alphaSearcher = new QueryExpandingAlphaSearcher("AlphaSearcher", indexReader);
		
		SearcherEvaluator eval = new SearcherEvaluator(alphaSearcher);
		
		Map<Query, Set<Result>> expectedResults = 
				SearcherEvaluator.importExpected(queriesFile, resultsFile);
		
		for (float min = 60 * 1; min < 60 * 10; min += 60 * 1) {
			for (float max = 60 * 5; max < 60 * 30; max += 60 * 5) {
				Vector results =
						eval.evaluateAgainstExpectedResults(expectedResults, WINDOW);
				
				System.out.println(min + ", " +
								   max + ", " +
								   results.getElement(0) + ", " +
								   results.getElement(1) + ", " +
								   results.getElement(2) + ", " +
								   f1Score(results));
			}
		}
		
	}
	
	private static double f1Score(Vector results) {
		return 2 * results.getElement(0) * results.getElement(1) /
			   (results.getElement(0) + results.getElement(1));
	}
}
