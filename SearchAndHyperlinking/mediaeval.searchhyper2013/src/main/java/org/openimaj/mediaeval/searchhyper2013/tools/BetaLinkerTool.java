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
import org.openimaj.mediaeval.searchhyper2013.datastructures.Anchor;
import org.openimaj.mediaeval.searchhyper2013.datastructures.AnchorList;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Result;
import org.openimaj.mediaeval.searchhyper2013.datastructures.ResultList;
import org.openimaj.mediaeval.searchhyper2013.linker.BetaLinker;
import org.openimaj.mediaeval.searchhyper2013.linker.LinkerException;
import org.openimaj.mediaeval.searchhyper2013.searcher.EpsilonSearcher;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherEvaluator;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;
import org.openimaj.mediaeval.searchhyper2013.util.LSHDataExplorer;
import org.xml.sax.SAXException;

public class BetaLinkerTool {
	public static final int WINDOW = 300;

	public static void main(String[] args) throws IOException, SearcherException, ParserConfigurationException, SAXException, LinkerException {
		/*if (args[0].equalsIgnoreCase("Search")) {
			search(new File(args[1]), args[2], new File(args[3]), new File(args[4]), new File(args[5]), new File(args[6]));
		} else*/ if (args[0].equalsIgnoreCase("Evaluate")) {
			evaluate(new File(args[1]), new File(args[2]), new File(args[3]), new File(args[4]), new File(args[5]), new File(args[6]), new File(args[7]));
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
								 File anchorsFile,
								 File shotsDirCacheFile,
								 File graphFile,
								 File conceptsDir,
								 File conceptsFile,
								 File synFile) 
										 throws IOException,
										 		ParserConfigurationException,
										 		SAXException, LinkerException {
		Directory indexDir = FSDirectory.open(index);
		IndexReader indexReader = DirectoryReader.open(indexDir);
		
		LSHDataExplorer lshExplorer = new LSHDataExplorer(graphFile, 10);
		
		EpsilonSearcher epsilonSearcher = new EpsilonSearcher("EpsilonSearcher", indexReader, shotsDirCacheFile, lshExplorer, conceptsDir, conceptsFile, synFile);
		
		BetaLinker betaLinker = new BetaLinker("BetaLinker", shotsDirCacheFile, lshExplorer, epsilonSearcher, indexReader);
		
		AnchorList anchors = AnchorList.readFromFile(anchorsFile);
		
		for (Anchor anchor : anchors) {
			System.out.println(anchor + " : ");
			
			ResultList results = betaLinker.link(anchor);
			
			System.out.println(results + "\n----");
		}
	}

}
