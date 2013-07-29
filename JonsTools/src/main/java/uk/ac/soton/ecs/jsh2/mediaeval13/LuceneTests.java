package uk.ac.soton.ecs.jsh2.mediaeval13;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;

public class LuceneTests {
	public static void main(String[] args) throws IOException, ParseException {
		final Directory directory = new SimpleFSDirectory(new File("/Users/jon/lucene-test-index"));

		final Query q = new QueryParser(Version.LUCENE_43, "tags", new StandardAnalyzer(Version.LUCENE_43))
				.parse("+southampton +snow");

		// 3. search
		final int hitsPerPage = 100000;
		final IndexReader reader = DirectoryReader.open(directory);
		final IndexSearcher searcher = new IndexSearcher(reader);
		final TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
		searcher.search(q, collector);
		final ScoreDoc[] hits = collector.topDocs().scoreDocs;

		// 4. display results
		System.out.println("Found " + collector.getTotalHits() + " hits.");
		final FImage img = new FImage(360, 180);
		for (int i = 0; i < hits.length; ++i) {
			final int docId = hits[i].doc;
			final Document d = searcher.doc(docId);
			final String[] llstr = d.get("location").split(" ");
			final float x = Float.parseFloat(llstr[0]) + 180;
			final float y = 90 - Float.parseFloat(llstr[1]);

			img.pixels[(int) (y % img.height)][(int) (x % img.width)] = 1;
		}
		DisplayUtilities.display(img.normalise());

		// reader can only be closed when there
		// is no need to access the documents any more.
		reader.close();
	}
}
