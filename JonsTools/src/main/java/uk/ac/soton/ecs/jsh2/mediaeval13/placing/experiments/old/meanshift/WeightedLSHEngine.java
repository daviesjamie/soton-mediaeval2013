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
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;

public class WeightedLSHEngine extends BasicLSHEngine {
	public WeightedLSHEngine(File luceneIndex, File edgesFile, TLongArrayList skipIds, int minEdgeWeight,
			int sampleCount,
			double bandwidth) throws IOException
	{
		super(luceneIndex, edgesFile, skipIds, minEdgeWeight, sampleCount, bandwidth, false);
	}

	@Override
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

				// add in proportion to the score
				for (int j = 0; j < hits[i].score; j++)
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
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}
