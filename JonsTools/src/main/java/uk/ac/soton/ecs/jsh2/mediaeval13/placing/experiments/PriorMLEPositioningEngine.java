package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;

import org.openimaj.image.FImage;
import org.openimaj.image.pixel.FValuePixel;

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
public class PriorMLEPositioningEngine implements GeoPositioningEngine {
	private GeoLocation mle;

	public PriorMLEPositioningEngine(File latlngFile, TLongArrayList skipIds) throws IOException {
		skipIds.sort();

		final FImage prior = Utils.createPrior(latlngFile, skipIds);
		final FValuePixel mp = prior.maxPixel();
		this.mle = new GeoLocation(90 - 180 / prior.height * mp.y, 360 / prior.width * mp.x - 180);

		System.out.println(mle);
	}

	@Override
	public GeoLocationEstimate estimateLocation(QueryImageData query) {
		return new GeoLocationEstimate(mle.latitude, mle.longitude, Math.random());
	}
}
