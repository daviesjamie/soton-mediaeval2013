package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

/**
 * @author Jonathan Hare (jsh2@ecs.soton.ac.uk), Sina Samangooei (ss@ecs.soton.ac.uk), David Duplaw (dpd@ecs.soton.ac.uk)
 *
 */
public class SolrIndexCompare {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String i1 = args[0];
		String i2 = args[1];
		final Directory directory1 = new SimpleFSDirectory(new File(i1));
		final IndexReader reader1 = DirectoryReader.open(directory1);
		final IndexSearcher searcher1 = new IndexSearcher(reader1);
		System.out.println(reader1.numDocs());
		final Directory directory2 = new SimpleFSDirectory(new File(i2));
		final IndexReader reader2 = DirectoryReader.open(directory2);
		System.out.println(reader2.numDocs());
		final IndexSearcher searcher2 = new IndexSearcher(reader2);
		TopScoreDocCollector collector = null;
		for (long i = 0; i < 300000; i++) {
			Query q = NumericRangeQuery.newLongRange("index", i, i, true, true);
			collector = TopScoreDocCollector.create(1, true);
			searcher1.search(q, collector);
			final ScoreDoc[] hits1 = collector.topDocs().scoreDocs;
			collector = TopScoreDocCollector.create(1, true);
			searcher2.search(q, collector);
			final ScoreDoc[] hits2 = collector.topDocs().scoreDocs;
			final Document d1 = searcher1.doc(hits1[0].doc);
			final Document d2 = searcher2.doc(hits2[0].doc);

			if(!d1.get("id").equals(d2.get("id"))){
				throw new Exception("Indexes are not the same!");
			}

		}
	}
}
