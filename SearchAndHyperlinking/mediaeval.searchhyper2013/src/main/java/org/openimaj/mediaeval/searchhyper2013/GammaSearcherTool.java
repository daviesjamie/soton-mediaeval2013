package org.openimaj.mediaeval.searchhyper2013;

import gov.sandia.cognition.math.matrix.Vector;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.xml.sax.SAXException;

public class GammaSearcherTool {
	public static final int WINDOW = 300;

	public static void main(String[] args) throws IOException, SearcherException, ParserConfigurationException, SAXException {
		if (args[0].equalsIgnoreCase("Search")) {
			search(new File(args[1]), args[2], new File(args[3]), new File(args[4]));
		} else if (args[0].equalsIgnoreCase("Evaluate")) {
			evaluate(new File(args[1]), new File(args[2]), new File(args[3]), new File(args[4]), new File(args[5]));
		} else {
			System.err.println("Unrecognised mode. Recognised modes are " + 
							   "'Search' and 'Evaluate'.");
		}
	}
	
	private static void search(File index, String query, File imageIndex, File shotsDirCacheFile) throws IOException, SearcherException {
		Directory indexDir = FSDirectory.open(index);
		IndexReader indexReader = DirectoryReader.open(indexDir);
		
		Directory imageIndexDir = FSDirectory.open(imageIndex);
		IndexReader imageIndexReader = DirectoryReader.open(imageIndexDir);
		
		GammaSearcher gammaSearcher = new GammaSearcher("GammaSearcher", indexReader, imageIndexReader, shotsDirCacheFile);
		
		Query q = new Query("CLI", query, null);
		
		System.out.println(gammaSearcher.search(q));
	}
	
	private static void evaluate(File index,
								 File queriesFile,
								 File resultsFile,
								 File imageIndex,
								 File shotsDirCacheFile) 
										 throws IOException,
										 		ParserConfigurationException,
										 		SAXException {
		System.out.println("Opening index...");
		Directory indexDir = FSDirectory.open(index);
		IndexReader indexReader = DirectoryReader.open(indexDir);
		
		System.out.println("Opening image index...");
		Directory imageIndexDir = FSDirectory.open(imageIndex);
		IndexReader imageIndexReader = DirectoryReader.open(imageIndexDir);
		
		GammaSearcher gammaSearcher = new GammaSearcher("GammaSearcher", indexReader, imageIndexReader, shotsDirCacheFile);
		
		SearcherEvaluator eval = new SearcherEvaluator(gammaSearcher);
		
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
		/*Vector evaluation =
				eval.evaluateAgainstExpectedResults(expectedResults, WINDOW);
		
		System.out.println(evaluation);
		System.out.println(SearcherEvaluator.f1Score(evaluation));*/
	}

}
