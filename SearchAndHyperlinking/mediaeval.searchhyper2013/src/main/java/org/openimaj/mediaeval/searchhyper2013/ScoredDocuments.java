package org.openimaj.mediaeval.searchhyper2013;

import java.util.HashMap;

import org.apache.lucene.document.Document;

public class ScoredDocuments extends HashMap<Document, Float> {

	public ScoredDocuments(int size) {
		super(size);
	}

}
