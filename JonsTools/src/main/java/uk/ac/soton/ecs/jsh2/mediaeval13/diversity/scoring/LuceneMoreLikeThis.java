package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.scoring;

import java.io.StringReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultList;

public class LuceneMoreLikeThis extends LuceneReranker {
	@Override
	protected Query buildQuery(ResultList rl, IndexReader reader) throws Exception {
		final MoreLikeThis mlt = new MoreLikeThis(reader);
		mlt.setAnalyzer(new StandardAnalyzer(Version.LUCENE_43));
		mlt.setFieldNames(new String[] { FIELD_DESCR, FIELD_TITLE, FIELD_TAGS });

		return mlt.like(new StringReader(StringUtils.stripAccents(rl.wikiItem.getWikiText())), FIELD_DESCR);
	}
}
