package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.scoring;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.openimaj.util.pair.ObjectDoublePair;
import org.terrier.utility.ArrayUtils;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultList;

public class LuceneReranker implements Scorer {
	protected static final String FIELD_TAGS = "TAGS";
	protected static final String FIELD_DESCR = "DESCRIPTION";
	protected static final String FIELD_TITLE = "TITLE";
	protected static final String FIELD_ID = "ID";

	@Override
	public List<ObjectDoublePair<ResultItem>> score(ResultList input) {
		try {
			final File index = new File(input.base, "lucene" + File.separator + input.monument);

			if (!index.exists())
				buildIndex(index, input);

			final TLongObjectHashMap<ResultItem> idMap = new TLongObjectHashMap<ResultItem>();
			for (final ResultItem ri : input)
				idMap.put(ri.id, ri);

			final Directory directory = new MMapDirectory(index);
			final IndexReader reader = DirectoryReader.open(directory);
			final IndexSearcher searcher = new IndexSearcher(reader);

			final Query q = buildQuery(input, reader);
			final TopDocs results = searcher.search(q, input.size());

			final List<ObjectDoublePair<ResultItem>> finalResult = new ArrayList<ObjectDoublePair<ResultItem>>();
			for (int i = 0; i < results.scoreDocs.length; i++) {
				final float score = results.scoreDocs[i].score;
				final Document doc = searcher.doc(results.scoreDocs[i].doc);
				final ResultItem res = idMap.get(Long.parseLong(doc.get(FIELD_ID)));

				if (res != null)
					finalResult.add(ObjectDoublePair.pair(res, score));
			}

			System.out.println(input.size() + " -> " + finalResult.size());
			System.out.println(ObjectDoublePair.getSecond(finalResult));

			return finalResult;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected Query buildQuery(ResultList rl, IndexReader reader) throws Exception {
		final String monument = StringUtils.stripAccents(rl.monument);

		final String queryStr = "("
				+ "TITLE:\"" + monument + "\"~20"
				+ " OR "
				+ "TAGS:\"" + monument + "\"~20"
				+ " OR "
				+ "DESCRIPTION:\"" + monument + "\"~20"
				+ ")"
				+ " OR "
				+ "(TITLE:" + monument + ")^0.5"
				+ "(TAGS:" + monument + ")^0.1"
		// + "(DESCRIPTION:" + monument + ")^0.1"
		;

		return new QueryParser(Version.LUCENE_43, FIELD_TITLE, new StandardAnalyzer(Version.LUCENE_43)).parse(queryStr);
	}

	private void buildIndex(File index, ResultList input) throws IOException {
		index.getParentFile().mkdirs();

		final StandardAnalyzer a = new StandardAnalyzer(Version.LUCENE_43);
		final IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_43, a);
		iwc.setRAMBufferSizeMB(512);
		final Directory directory = new MMapDirectory(index);
		final IndexWriter indexWriter = new IndexWriter(directory, iwc);

		for (final ResultItem ri : input) {
			final Document doc = makeDocument(ri);
			indexWriter.addDocument(doc);
		}

		indexWriter.commit();
		indexWriter.close();
	}

	private Document makeDocument(ResultItem ri) {
		final Document doc = new Document();

		doc.add(new LongField(FIELD_ID, ri.id, Store.YES));
		doc.add(new TextField(FIELD_TITLE, StringUtils.stripAccents(ri.title), Store.YES));
		doc.add(new TextField(FIELD_DESCR, StringUtils.stripAccents(ri.description), Store.YES));
		doc.add(new TextField(FIELD_TAGS, StringUtils.stripAccents(ArrayUtils.join(ri.tags, " ")), Store.YES));

		return doc;
	}
}
