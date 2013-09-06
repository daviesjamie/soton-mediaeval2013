package org.openimaj.mediaeval.searchhyper2013.tools;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class DictionaryTool {

	public static void main(String[] args) throws IOException {
		IndexWriterConfig indexWriterConfig =
				new IndexWriterConfig(Version.LUCENE_43,
									  new EnglishAnalyzer(Version.LUCENE_43));
		
		System.out.println("Building spellchecker index...");
		Directory spellDir = FSDirectory.open(new File(args[0]));
		
		SpellChecker spellChecker = new SpellChecker(spellDir);
		
		for (int i = 1; i < args.length; i++) {
			try {
				spellChecker.indexDictionary(
						new PlainTextDictionary(new File(args[i])),
						indexWriterConfig,
						false);
			} finally {
				System.out.println(args[i]);
			}
		}
		
		spellChecker.close();
	}

}
