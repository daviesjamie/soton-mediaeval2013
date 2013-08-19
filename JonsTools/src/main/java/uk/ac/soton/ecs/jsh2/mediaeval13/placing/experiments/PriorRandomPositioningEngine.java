package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoPositioningEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.util.Utils;

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
		pts = Utils.readLatLng(latlngFile, skipIds);
	}

	@Override
	public GeoLocationEstimate estimateLocation(QueryImageData query) {
		final int r = rnd.nextInt(pts.size());
		final GeoLocation loc = pts.get(r);

		return new GeoLocationEstimate(loc.latitude, loc.longitude, Math.random());
	}
}
