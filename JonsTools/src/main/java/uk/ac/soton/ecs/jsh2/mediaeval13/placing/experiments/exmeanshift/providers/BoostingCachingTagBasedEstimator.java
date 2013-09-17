package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.IndexSearcher;
import org.openimaj.util.iterator.TextLineIterable;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;

public class BoostingCachingTagBasedEstimator extends CachingTagBasedEstimator {
	TIntObjectHashMap<GeoLocation> records = new TIntObjectHashMap<GeoLocation>();
	Map<String, TIntHashSet> index = new HashMap<String, TIntHashSet>();
	long maxPop;

	public BoostingCachingTagBasedEstimator(IndexSearcher searcher, File cacheLocation) {
		super(searcher, cacheLocation);

		readGeoNamesPlaces();
	}

	void readGeoNamesPlaces() {
		final File f = new File("/Volumes/SSD/mediaeval13/placing/geonames-places.txt");

		int c = 0;
		for (final String line : new TextLineIterable(f, "utf-8")) {
			if (c++ % 1000 == 0)
				System.err.println(c);

			final String[] parts = line.split("\t");

			final GeoLocation rec = new GeoLocation(Double.parseDouble(parts[4]), Double.parseDouble(parts[5]));

			final String name = parts[1].replaceAll("\\s+", "").trim().toLowerCase();
			final String asciiname = parts[2].replaceAll("\\s+", "").trim().toLowerCase();
			final String[] alternateNames = parts[3].split(",");
			for (int i = 0; i < alternateNames.length; i++)
				alternateNames[i] = alternateNames[i].replaceAll("\\s+", "").trim().toLowerCase();

			final int id = Integer.parseInt(parts[0]);
			records.put(id, rec);

			if (!index.containsKey(name))
				index.put(name, new TIntHashSet());
			index.get(name).add(id);

			if (asciiname.length() > 0) {
				if (!index.containsKey(asciiname))
					index.put(asciiname, new TIntHashSet());
				index.get(asciiname).add(id);
			}

			for (final String n : alternateNames) {
				if (n.length() > 0) {
					if (!index.containsKey(n))
						index.put(n, new TIntHashSet());
					index.get(n).add(id);
				}
			}
		}
	}

	@Override
	protected List<GeoLocation> search(String query) {
		final List<GeoLocation> locs = super.search(query);

		final TIntHashSet ids = index.get(query);
		if (ids != null) {
			if (locs.size() != 0) {
				locs.addAll(locs);
			} else {
				for (final int id : ids.toArray()) {
					locs.add(records.get(id));
				}
				final int sz = locs.size();
				while (locs.size() < this.sampleCount) {
					locs.add(locs.get((int) (Math.random() * sz)));
				}
			}
		}

		return locs;
	}
}
