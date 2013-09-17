package uk.ac.soton.ecs.jsh2.mediaeval13.placing.playground;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

/**
 * Demonstrate simple Lucene search
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class LucenePlayground {
	public static void main(String[] args) throws IOException, ParseException {
		final Directory directory = new SimpleFSDirectory(new File("../Placement/data/bigdatasetutf8-2.lucene"));

//		for (int j = 0; j < 10000; j++) {
			// final Query q = new QueryParser(Version.LUCENE_43, "tags", new
			// StandardAnalyzer(Version.LUCENE_43))
			// .parse("+snow");
////			final DateTime dateTime = DateTime.parse("2008-01-01T00:01:00");
////			final long startDate = dateTime.plusMinutes(j * 10).getMillis() / 1000;
////			final long stopDate = dateTime.plusMinutes(j * 10 + 10).getMillis() / 1000;
////			final Query q = NumericRangeQuery.newLongRange(LuceneIndexBuilder.FIELD_TAKEN, startDate, stopDate, true,
////					true);
//
//			// 3. search
//			final int hitsPerPage = 100000;
			final IndexReader reader = DirectoryReader.open(directory);
			System.out.println( reader.maxDoc() );
//			final IndexSearcher searcher = new IndexSearcher(reader);
//			final TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
//			searcher.search(q, collector);
//			final ScoreDoc[] hits = collector.topDocs().scoreDocs;
//
//			// 4. display results
//			System.out.println("Found " + collector.getTotalHits() + " hits.");
//			final FImage img = new FImage(360, 180);
//			for (int i = 0; i < hits.length; ++i) {
//				final int docId = hits[i].doc;
//				final Document d = searcher.doc(docId);
//				final String[] llstr = d.get("location").split(" ");
//				final float x = Float.parseFloat(llstr[0]) + 180;
//				final float y = 90 - Float.parseFloat(llstr[1]);
//
//				img.pixels[(int) (y % img.height)][(int) (x % img.width)] = 1;
//			}
//			DisplayUtilities.displayName(img.normalise(), "foo");
//
//			// reader can only be closed when there
//			// is no need to access the documents any more.
//			reader.close();
//		}
	}
}
