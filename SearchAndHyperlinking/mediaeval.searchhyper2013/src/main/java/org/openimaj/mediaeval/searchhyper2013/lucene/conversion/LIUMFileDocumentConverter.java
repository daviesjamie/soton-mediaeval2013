package org.openimaj.mediaeval.searchhyper2013.lucene.conversion;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.FieldInfo.IndexOptions;
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
        

		FieldType fieldType = new FieldType();
		fieldType.setIndexed(true);
		fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		fieldType.setStored(true);
		fieldType.setStoreTermVectors(true);
		fieldType.setStoreTermVectorOffsets(true);
		fieldType.setStoreTermVectorPositions(true);
		fieldType.setStoreTermVectorPayloads(true);
		fieldType.setTokenized(true);
		fieldType.freeze();
	
		doc.add(new org.apache.lucene.document.Field(Field.Text.toString(),
						  							 wordsBuilder.toString(),
						  							 fieldType));
        doc.add(new StringField(Field.Times.toString(),
        						timesBuilder.toString(),
        						org.apache.lucene.document.Field.Store.YES));
        
        return doc;
	}

}
