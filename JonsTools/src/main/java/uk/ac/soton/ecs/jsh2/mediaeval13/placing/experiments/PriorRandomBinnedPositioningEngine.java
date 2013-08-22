package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.openimaj.image.FImage;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.util.Utils;

/**
 * This just guesses locations randomly with a prior bias based on the
 * distribution of photos in the world - i.e. it's more likely to pick somewhere
 * with more photos!
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 */
public class PriorRandomBinnedPositioningEngine extends PriorRandomPositioningEngine {
	public PriorRandomBinnedPositioningEngine(File latlngFile, TLongArrayList skipIds) throws IOException {
		skipIds.sort();

		pts = new ArrayList<GeoLocation>();

		final FImage prior = Utils.createPrior(latlngFile, skipIds, false);
		final float min = 0;
		final float max = prior.max();

		for (int r = 0; r < prior.height; r++)
		{
			for (int c = 0; c < prior.width; c++)
			{
				final int val = (int) (1000 * (prior.pixels[r][c] - min) / (max - min));

				for (int i = 0; i < val; i++) {
					pts.add(new GeoLocation(90 - 180 / prior.height * r, 360 / prior.width * c - 180));
				}
			}
		}

	}
}
