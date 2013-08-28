package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.old.meanshift;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.LSHSiftGraphSearcher;

public class BasicLSHEngine extends MeanShiftTagEngine {
	protected LSHSiftGraphSearcher visualSearcher;

	public BasicLSHEngine(File luceneIndex, File edgesFile, TLongArrayList skipIds, int minEdgeWeight, int sampleCount,
			double bandwidth, boolean expand) throws IOException
	{
		super(luceneIndex, skipIds, sampleCount, null, bandwidth);

		this.visualSearcher = new LSHSiftGraphSearcher(edgesFile, minEdgeWeight, searcher);
		visualSearcher.setExpand(expand);
	}

	protected List<GeoLocation> search(long query) {
		try {
			final ScoreDoc[] hits = visualSearcher.search(query, 100000);

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
				while (locations.size() < sampleCount) {
					locations.add(locations.get((int) (Math.random() * locations.size())));
				}
			}

			return locations;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public GeoLocationEstimate estimateLocation(QueryImageData query) {
		final List<GeoLocation> pts = search(query.flickrId);

		if (prior != null) {
			pts.addAll(prior);
		}

		if (pts.size() == 0)
			return new GeoLocationEstimate(0, 0, Math.random() * 1000);
		if (allSame(pts))
			return new GeoLocationEstimate(pts.get(0).latitude, pts.get(0).longitude, 0);

		return computeLocationMeanShift(pts);
	}

	private boolean allSame(List<GeoLocation> pts) {
		final GeoLocation first = pts.get(0);
		for (int i = 1; i < pts.size(); i++) {
			final GeoLocation pt = pts.get(i);
			if (first.latitude != pt.latitude || first.longitude != pt.longitude)
				return false;
		}
		return true;
	}
}
