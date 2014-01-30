package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers;

import gnu.trove.list.array.TLongArrayList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.openimaj.image.MBFImage;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.VisualSearcher;

public class ScoreWeightedVisualEstimator implements GeoDensityEstimateProvider {
	private TLongArrayList skipIds;
	private int sampleCount;
	private IndexSearcher searcher;
	private VisualSearcher visualSearcher;
	private int numResults;
	private float scaling;

	public ScoreWeightedVisualEstimator(IndexSearcher searcher, VisualSearcher visualSearcher, int numResults,
			float scaling)
	{
		this.searcher = searcher;
		this.visualSearcher = visualSearcher;
		this.numResults = numResults;
		this.scaling = scaling;
	}

	@Override
	public void setSkipIds(TLongArrayList skipIds) {
		this.skipIds = skipIds;
	}

	@Override
	public void setSampleCount(int sampleCount) {
		this.sampleCount = sampleCount;
	}

	@Override
	public List<GeoLocation> estimatePoints(QueryImageData query) {
		try {
			final ScoreDoc[] hits = visualSearcher.search(query.flickrId, numResults);

			return buildResults(hits);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private List<GeoLocation> buildResults(final ScoreDoc[] hits) throws IOException {
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

			// add in proportion to the score
			for (int j = 0; j < this.scaling * hits[i].score; j++)
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

		return locations;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ScoreWeightedVisualEstimator[visualSearcher=" + visualSearcher
				+ ", numResults=" + numResults + ", scaling=" + scaling + "]";
	}

	@Override
	public List<GeoLocation> estimatePoints(MBFImage image, String[] tags) {
		try {
			final ScoreDoc[] hits = visualSearcher.search(image, numResults);

			return buildResults(hits);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

}
