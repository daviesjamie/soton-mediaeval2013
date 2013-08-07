package org.openimaj.mediaeval.searchhyper2013;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.openimaj.io.FileUtils;

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
		
		List<File> synopsisDocs = removeExcluded(new File(args[2]), excludes);
		List<File> subsDocs = removeExcluded(new File(args[3]), excludes);
		List<File> limsiDocs = removeExcluded(new File(args[4]), excludes);
		List<File> liumDocs = removeExcluded(new File(args[5]), excludes);
		
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
	
	public static List<File> removeExcluded(File dir, List<String> excludes) {
		List<File> files = new ArrayList<File>();
		for (File file : dir.listFiles()) {
			if (!excludes.contains(file.getName().split("\\.")[0])) {
				files.add(file);
			}
		}
		
		return files;
	}
}
