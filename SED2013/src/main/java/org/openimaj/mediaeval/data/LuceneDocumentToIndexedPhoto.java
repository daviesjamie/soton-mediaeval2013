package org.openimaj.mediaeval.data;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.solr.common.SolrDocument;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.util.function.Function;

/**
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class LuceneDocumentToIndexedPhoto implements Function<Document, IndexedPhoto> {
	@Override
	public IndexedPhoto apply(Document in) {
		return IndexedPhoto.fromDoc(in);
	}
}