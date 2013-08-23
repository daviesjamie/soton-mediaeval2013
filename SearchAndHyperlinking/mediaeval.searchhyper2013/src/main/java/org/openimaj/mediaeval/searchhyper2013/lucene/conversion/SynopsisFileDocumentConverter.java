package org.openimaj.mediaeval.searchhyper2013.lucene.conversion;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
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

		Document doc = new Document();
		doc.add(new StringField(Field.Program.toString(),
								(String) deser.get("filename"),
								org.apache.lucene.document.Field.Store.YES));
		doc.add(new TextField(Field.Text.toString(),
							  (String) deser.get("description"),
							  org.apache.lucene.document.Field.Store.YES));
		doc.add(new StringField(Field.Type.toString(),
								Type.Synopsis.toString(),
								org.apache.lucene.document.Field.Store.YES));
		doc.add(new FloatField(Field.Length.toString(),
							   Float.parseFloat((String) deser.get("duration")),
							   org.apache.lucene.document.Field.Store.YES));
		doc.add(new StringField(Field.Title.toString(),
								(String) deser.get("title"),
								org.apache.lucene.document.Field.Store.YES));
		
		return doc;
	}

}
