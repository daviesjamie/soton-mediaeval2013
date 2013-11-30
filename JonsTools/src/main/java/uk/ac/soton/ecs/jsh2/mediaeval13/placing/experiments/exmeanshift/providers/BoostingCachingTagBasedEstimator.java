package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.IndexSearcher;
import org.openimaj.io.IOUtils;
import org.openimaj.util.iterator.TextLineIterable;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;

public class BoostingCachingTagBasedEstimator extends CachingTagBasedEstimator {
	public static class Data {
		TIntObjectHashMap<GeoLocation> records = new TIntObjectHashMap<GeoLocation>();
		Map<String, TIntHashSet> index = new HashMap<String, TIntHashSet>();
	}

	Data data;

	public BoostingCachingTagBasedEstimator(IndexSearcher searcher, File cacheLocation) {
		super(searcher, cacheLocation);

		// data = readGeoNamesPlacesRaw();
		try {
			System.err.println("Reading geonames index...");
			data = IOUtils.readFromFile(new File("/Volumes/SSD/mediaeval13/placing/geonames-places.bin"));
			System.err.println("Done");
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	static Data readGeoNamesPlacesRaw() {
		final Data data = new Data();
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
			data.records.put(id, rec);

			if (!data.index.containsKey(name))
				data.index.put(name, new TIntHashSet());
			data.index.get(name).add(id);

			if (asciiname.length() > 0) {
				if (!data.index.containsKey(asciiname))
					data.index.put(asciiname, new TIntHashSet());
				data.index.get(asciiname).add(id);
			}

			for (final String n : alternateNames) {
				if (n.length() > 0) {
					if (!data.index.containsKey(n))
						data.index.put(n, new TIntHashSet());
					data.index.get(n).add(id);
				}
			}
		}
		return data;
	}

	@Override
	protected List<GeoLocation> search(String query) {
		final List<GeoLocation> locs = super.search(query);

		final TIntHashSet ids = data.index.get(query);
		if (ids != null) {
			if (locs.size() != 0) {
				locs.addAll(locs);
			} else {
				for (final int id : ids.toArray()) {
					locs.add(data.records.get(id));
				}
				final int sz = locs.size();

				if (sz == 0)
					return locs;

				while (locs.size() < this.sampleCount) {
					locs.add(locs.get((int) (Math.random() * sz)));
				}
			}
		}

		return locs;
	}

	public static void main(String[] args) throws IOException {
		final Data data = readGeoNamesPlacesRaw();

		IOUtils.writeToFile(data, new File("/Volumes/SSD/mediaeval13/placing/geonames-places.bin"));
	}
}
