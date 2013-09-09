package org.openimaj.mediaeval.searchhyper2013.lucene.conversion;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.openimaj.io.FileUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

public class SynopsisFileDocumentConverter implements FileDocumentConverter {

	@Override
	public Document convertFile(File synopsisFile)
			throws FileDocumentConverterException {
		System.out.println("(Synopsis) " + synopsisFile.getName());
		
		Gson gson = new Gson();
		
		String json;
		try {
			json = FileUtils.readall(synopsisFile);
		} catch (IOException e) {
			throw new FileDocumentConverterException(e);
		}
		
		@SuppressWarnings("rawtypes")
		LinkedTreeMap deser = gson.fromJson(json, LinkedTreeMap.class);

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
		
		Document doc = new Document();
		doc.add(new StringField(Field.Program.toString(),
								(String) deser.get("filename"),
								org.apache.lucene.document.Field.Store.YES));
		doc.add(new org.apache.lucene.document.Field(Field.Text.toString(),
													 (String) deser.get("description"),
													 fieldType));
		doc.add(new StringField(Field.Type.toString(),
								Type.Synopsis.toString(),
								org.apache.lucene.document.Field.Store.YES));
		doc.add(new FloatField(Field.Length.toString(),
							   Float.parseFloat((String) deser.get("duration")),
							   org.apache.lucene.document.Field.Store.YES));
		doc.add(new org.apache.lucene.document.Field(Field.Title.toString(),
													 (String) deser.get("title"),
													 fieldType));
		
		return doc;
	}

}
