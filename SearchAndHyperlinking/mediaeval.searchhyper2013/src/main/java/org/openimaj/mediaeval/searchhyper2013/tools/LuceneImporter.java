package org.openimaj.mediaeval.searchhyper2013.tools;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.openimaj.io.FileUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.conversion.FileDocumentConverterException;
import org.openimaj.mediaeval.searchhyper2013.lucene.conversion.LIMSIFileDocumentConverter;
import org.openimaj.mediaeval.searchhyper2013.lucene.conversion.LIUMFileDocumentConverter;
import org.openimaj.mediaeval.searchhyper2013.lucene.conversion.SubtitlesFileDocumentConverter;
import org.openimaj.mediaeval.searchhyper2013.lucene.conversion.SynopsisFileDocumentConverter;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;
import org.openimaj.mediaeval.searchhyper2013.util.Files;

/**
 * Imports all transcripts and synopsis data into a Lucene index.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class LuceneImporter {
	public static void main(String[] args) throws IOException, FileDocumentConverterException, SearcherException {
		Directory indexDir = FSDirectory.open(new File(args[0]));
		IndexWriter writer = 
				new IndexWriter(indexDir,
								new IndexWriterConfig(Version.LUCENE_43,
								new EnglishAnalyzer(Version.LUCENE_43)));
		
		SubtitlesFileDocumentConverter subsDocConv = 
				new SubtitlesFileDocumentConverter(); 
		SynopsisFileDocumentConverter synopsDocConv = 
				new SynopsisFileDocumentConverter();
		LIMSIFileDocumentConverter limsiDocConv = 
				new LIMSIFileDocumentConverter();
		LIUMFileDocumentConverter liumDocConv = 
				new LIUMFileDocumentConverter();
		
		List<String> excludes = Arrays.asList(FileUtils.readlines(new File(args[1])));
		
		List<File> synopsisDocs = Files.removeExcluded(new File(args[2]), excludes);
		List<File> subsDocs = Files.removeExcluded(new File(args[3]), excludes);
		List<File> limsiDocs = Files.removeExcluded(new File(args[4]), excludes);
		List<File> liumDocs = Files.removeExcluded(new File(args[5]), excludes);
		
		for (File file : synopsisDocs) {
			Document doc = synopsDocConv.convertFile(file);
			
			writer.addDocument(doc);
		}
		
		for (File file : subsDocs) {
			Document doc = subsDocConv.convertFile(file);
			
			writer.addDocument(doc);
		}

		for (File file : limsiDocs) {
			Document doc = limsiDocConv.convertFile(file);
			
			writer.addDocument(doc);
		}
		
		for (File file : liumDocs) {
			Document doc = liumDocConv.convertFile(file);
			
			writer.addDocument(doc);
		}
		
		writer.close();
	}
}
