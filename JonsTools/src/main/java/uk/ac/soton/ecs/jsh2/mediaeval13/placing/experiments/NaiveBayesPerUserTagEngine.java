package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.openimaj.image.FImage;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;

public class NaiveBayesPerUserTagEngine extends NaiveBayesTagEngine {
	/**
	 * Construct with the given data. If the latlngFile is null, then the prior
	 * will be uniform.
	 * 
	 * @param file
	 * @param skipIds
	 * @param latlngFile
	 * @throws IOException
	 */
	public NaiveBayesPerUserTagEngine(File file, TLongArrayList skipIds, File latlngFile) throws IOException {
		super(file, skipIds, latlngFile);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected FImage search(String query) {
		try {
			final TermQuery q = new TermQuery(new Term(LuceneIndexBuilder.FIELD_TAGS, query));

			final TopScoreDocCollector collector = TopScoreDocCollector.create(1000000, true);
			searcher.search(q, collector);
			final ScoreDoc[] hits = collector.topDocs().scoreDocs;

			final Set<?>[][] users = new Set[180][360];
			for (int i = 0; i < hits.length; ++i) {
				final int docId = hits[i].doc;
				final Document d = searcher.doc(docId);

				final long flickrId = Long.parseLong(d.get(LuceneIndexBuilder.FIELD_ID));
				if (skipIds.contains(flickrId))
					continue;

				final String[] llstr = d.get(LuceneIndexBuilder.FIELD_LOCATION).split(" ");
				final float x = Float.parseFloat(llstr[0]) + 180;
				final float y = 90 - Float.parseFloat(llstr[1]);

				if (users[(int) (y % users.length)][(int) (x % users[0].length)] == null)
					users[(int) (y % users.length)][(int) (x % users[0].length)] = new HashSet<String>();

				((Set<String>) users[(int) (y % users.length)][(int) (x % users[0].length)]).add(d
						.get(LuceneIndexBuilder.FIELD_USER));
			}

			final FImage img = new FImage(360, 180);
			for (int y = 0; y < users.length; y++) {
				for (int x = 0; x < users[0].length; x++) {
					if (users[y][x] == null)
						img.pixels[y][x] = 1f / (img.height * img.width);
					else
						img.pixels[y][x] = users[y][x].size();
				}
			}
			logNorm(img);

			return img;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}
