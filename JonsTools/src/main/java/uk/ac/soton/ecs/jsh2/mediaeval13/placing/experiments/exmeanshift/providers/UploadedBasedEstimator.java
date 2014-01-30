package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers;

import gnu.trove.list.array.TLongArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.openimaj.image.MBFImage;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;

public class UploadedBasedEstimator implements GeoDensityEstimateProvider {

	protected static long WINDOW = 3600;
	protected static long DAYS = 7;

	protected IndexSearcher searcher;
	protected TLongArrayList skipIds;
	protected int sampleCount;

	public UploadedBasedEstimator(IndexSearcher searcher) {
		this.searcher = searcher;
	}

	protected List<GeoLocation> search(Long query, String user) {
		try {
			final BooleanQuery timeq = new BooleanQuery();
			for (long t = query - (DAYS * 86400); t < query + (DAYS * 86400); t += 86400) {
				final Query r = NumericRangeQuery.newLongRange(LuceneIndexBuilder.FIELD_UPLOADED, t - WINDOW, t + WINDOW,
						true, true);
				timeq.add(new BooleanClause(r, Occur.SHOULD));
			}

			final TermQuery userq = new TermQuery(new Term(LuceneIndexBuilder.FIELD_USER, user));

			final BooleanQuery q = new BooleanQuery();
			q.add(new BooleanClause(timeq, Occur.MUST));
			q.add(new BooleanClause(userq, Occur.MUST_NOT));

			final TopScoreDocCollector collector = TopScoreDocCollector.create(1000000, true);
			searcher.search(q, collector);
			final ScoreDoc[] hits = collector.topDocs().scoreDocs;

			List<GeoLocation> locations = new ArrayList<GeoLocation>(hits.length);
			for (int i = 0; i < hits.length; ++i) {
				final int docId = hits[i].doc;
				final Document d = searcher.doc(docId);

				final long flickrId = Long.parseLong(d.get(LuceneIndexBuilder.FIELD_ID));
				if (skipIds.contains(flickrId))
					continue;

				final String[] llstr = d.get(LuceneIndexBuilder.FIELD_LOCATION).split(" ");
				final float lon = Float.parseFloat(llstr[0]);
				final float lat = Float.parseFloat(llstr[1]);

				locations.add(new GeoLocation(lat, lon));
			}

			if (locations.size() == 0)
				return locations;

			if (locations.size() > sampleCount) {
				Collections.shuffle(locations);
				locations = locations.subList(0, sampleCount);
			} else {
				final int size = locations.size();
				while (locations.size() < sampleCount) {
					locations.add(locations.get((int) (Math.random() * size)));
				}
			}

			// for( GeoLocation g : locations ) {
			// System.out.println( g );
			// }

			return locations;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<GeoLocation> estimatePoints(QueryImageData query) {
		final List<GeoLocation> pts = search(query.dateUploaded.getTime(), query.userId);
		return pts;
	}

	@Override
	public void setSampleCount(int sampleCount) {
		this.sampleCount = sampleCount;
	}

	@Override
	public void setSkipIds(TLongArrayList skipIds) {
		this.skipIds = skipIds;
	}

	@Override
	public List<GeoLocation> estimatePoints(MBFImage image, String[] tags) {
		throw new UnsupportedOperationException();
	}
}
