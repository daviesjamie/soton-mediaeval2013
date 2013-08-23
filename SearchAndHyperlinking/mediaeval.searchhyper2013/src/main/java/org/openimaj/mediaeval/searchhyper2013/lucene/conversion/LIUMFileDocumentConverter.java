package org.openimaj.mediaeval.searchhyper2013.lucene.conversion;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.openimaj.io.FileUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;

public class LIUMFileDocumentConverter implements FileDocumentConverter {

	@Override
	public Document convertFile(File liumFile)
			throws FileDocumentConverterException {
        System.out.println("(LIUM) " + liumFile.getName());
        
        String[] lines;
		try {
			lines = FileUtils.readlines(liumFile);
		} catch (IOException e) {
			throw new FileDocumentConverterException(e);
		}
       
        String prog = lines[0].split(" ", 2)[0];

        Document doc = new Document();
        
        doc.add(new StringField(Field.Program.toString(),
        						prog,
        						org.apache.lucene.document.Field.Store.YES));
        doc.add(new StringField(Field.Type.toString(),
        						Type.LIUM.toString(),
        						org.apache.lucene.document.Field.Store.YES));
        
        StringBuilder wordsBuilder = new StringBuilder();
        StringBuilder timesBuilder = new StringBuilder();
        
        for (String line : lines) {
        	String[] components = line.split(" ");
        	
        	timesBuilder.append(Float.parseFloat(components[2]) + " ");
        	wordsBuilder.append(components[4] + " ");
        }
        
        doc.add(new TextField(Field.Text.toString(),
        					  wordsBuilder.toString(),
        					  org.apache.lucene.document.Field.Store.YES));
        doc.add(new StringField(Field.Times.toString(),
        						timesBuilder.toString(),
        						org.apache.lucene.document.Field.Store.YES));
        
        return doc;
	}

}
