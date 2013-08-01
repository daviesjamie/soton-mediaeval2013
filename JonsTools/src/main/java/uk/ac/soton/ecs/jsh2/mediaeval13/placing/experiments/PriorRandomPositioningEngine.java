package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments;

import gnu.trove.list.array.TLongArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoPositioningEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;

/**
 * This just guesses locations randomly with a prior bias based on the
 * distribution of photos in the world - i.e. it's more likely to pick somewhere
 * with more photos!
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 */
public class PriorRandomPositioningEngine implements GeoPositioningEngine {
	Random rnd = new Random();
	List<GeoLocation> pts;

	public PriorRandomPositioningEngine(File latlngFile, TLongArrayList skipIds) throws IOException {
		skipIds.sort();
		readLatLng(latlngFile, skipIds);
	}

	private void readLatLng(File latlngFile, TLongArrayList skipIds) throws FileNotFoundException, IOException {
		pts = new ArrayList<GeoLocation>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(latlngFile));

			String line;
			while ((line = br.readLine()) != null) {
				try {
					final String[] parts = line.split(" ");
					final long id = Long.parseLong(parts[0]);
					final double lat = Double.parseDouble(parts[1]);
					final double lng = Double.parseDouble(parts[2]);

					if (skipIds.binarySearch(id) < 0)
						pts.add(new GeoLocation(lat, lng));
				} catch (final NumberFormatException nfe) {
					// ignore line
				}
			}
		} finally {
			if (br != null)
				br.close();
		}
	}

	@Override
	public GeoLocationEstimate estimateLocation(QueryImageData query) {
		final int r = rnd.nextInt(pts.size());
		final GeoLocation loc = pts.get(r);

		return new GeoLocationEstimate(loc.latitude, loc.longitude, Math.random());
	}
}
