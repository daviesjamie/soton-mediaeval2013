package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.random;

import org.openimaj.image.MBFImage;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoPositioningEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;

/**
 * This picks locations uniformly at random, anywhere on the earths surface
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class RandomPositioningEngine implements GeoPositioningEngine {
	@Override
	public GeoLocationEstimate estimateLocation(QueryImageData query) {
		final double lat = (Math.random() * 180) - 90;
		final double lng = (Math.random() * 360) - 180;
		final double est = Math.random() * 1000;
		return new GeoLocationEstimate(lat, lng, est);
	}

	@Override
	public GeoLocationEstimate estimateLocation(MBFImage image, String[] tags) {
		return estimateLocation(null);
	}
}
