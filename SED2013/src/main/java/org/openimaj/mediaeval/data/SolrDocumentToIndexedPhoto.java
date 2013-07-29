package org.openimaj.mediaeval.data;

import org.apache.solr.common.SolrDocument;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.util.function.Function;

/**
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SolrDocumentToIndexedPhoto implements Function<SolrDocument, IndexedPhoto> {
	@Override
	public IndexedPhoto apply(SolrDocument in) {
		return IndexedPhoto.fromDoc(in);
	}
}