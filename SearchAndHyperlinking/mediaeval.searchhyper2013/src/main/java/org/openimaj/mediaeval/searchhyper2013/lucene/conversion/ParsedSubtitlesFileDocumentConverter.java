package org.openimaj.mediaeval.searchhyper2013.lucene.conversion;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.jfree.io.FileUtilities;
import org.openimaj.io.FileUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;
import org.openimaj.mediaeval.searchhyper2013.util.Time;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class ParsedSubtitlesFileDocumentConverter implements FileDocumentConverter {

	@Override
	public Document convertFile(File subsFile) throws FileDocumentConverterException {
		try {
			return _convertFile(subsFile);
		} catch (Exception e) {
			throw new FileDocumentConverterException(e);
		}
	}
	
	private Document _convertFile(File subsFile) throws Exception {
		System.out.println("(Parsed Subs) " + subsFile.getName());
		
		final Document doc = new Document();
		final String progName = subsFile.getName().split("\\.")[0];
		
		doc.add(new StringField(Field.Program.toString(),
								progName,
								org.apache.lucene.document.Field.Store.YES));
		doc.add(new StringField(Field.Type.toString(),
								Type.Subtitles.toString(),
								org.apache.lucene.document.Field.Store.YES));
		
		final StringBuilder words = new StringBuilder();
		final StringBuilder times = new StringBuilder();
		
		String[] lines = FileUtils.readlines(subsFile);
		
		int totalWords = 0;
		float totalTime = 0;
		
		for (int i = 0; i < lines.length - 1; i++) {
			String[] parts = lines[i].split(",", 1);
			
			float startTime = Time.MStoS(parts[0]);
			float endTime = Time.MStoS(lines[i + 1].split(",", 1)[0]);
			
			String[] sentence = parts[1].trim()
										.replaceAll("\\s+", " ")
										.split(" ");
			
			for (int j = 0; j < sentence.length; j++) {
				float wordTime = startTime + 
								 (j * (endTime - startTime) / sentence.length);
				
				words.append(sentence[j] + " ");
				times.append(wordTime + " ");
			}
			
			totalTime += endTime - startTime;
			totalWords += sentence.length;
		}
		
		String[] parts = lines[lines.length - 1].split(",", 1);
		
		float startTime = Time.MStoS(parts[0]);
		
		String[] finalSentence = parts[1].trim()
										 .replaceAll("\\s+", " ")
										 .split(" ");
		
		float timeDelta = totalTime / totalWords;
		
		for (int j = 0; j < finalSentence.length; j++) {
			float wordTime = startTime + (j * timeDelta);
			
			words.append(finalSentence[j] + " ");
			times.append(wordTime + " ");
		}
		
		doc.add(new TextField(Field.Text.toString(),
							  words.toString().replaceAll("\\s+", " ").trim(),
							  org.apache.lucene.document.Field.Store.YES));
		doc.add(new StringField(Field.Times.toString(),
				  				times.toString().trim(),
				  				org.apache.lucene.document.Field.Store.YES));
		
		return doc;
	}

}
