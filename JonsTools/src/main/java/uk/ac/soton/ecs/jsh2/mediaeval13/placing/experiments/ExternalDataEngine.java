package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments;

import gnu.trove.TLongObjectHashMap;

import java.io.File;
import java.io.IOException;

import org.openimaj.util.iterator.TextLineIterable;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoPositioningEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;

/**
 * {@link GeoPositioningEngine} that just gets it's estimates from an externally
 * provided data file with the format <code>id lon lat errAngle</code>.
 * Estimated error in kilometers will be computed using the average haversine
 * distance to a point +/- errAngle from the prediction.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class ExternalDataEngine implements GeoPositioningEngine {
	TLongObjectHashMap<GeoLocationEstimate> data = new TLongObjectHashMap<GeoLocationEstimate>();

	public ExternalDataEngine(File file) throws IOException {
		for (final String line : new TextLineIterable(file)) {
			final String[] parts = line.split(" ");

			final long id;
			if (parts[0].contains("/"))
				id = Long.parseLong(parts[0].substring(parts[0].indexOf("/") + 1, parts[0].indexOf(".")));
			else
				id = Long.parseLong(parts[0]);

			final double lat = Double.parseDouble(parts[2]);
			final double lng = Double.parseDouble(parts[1]);
			final double errAng = Double.parseDouble(parts[3]);

			final GeoLocationEstimate est = new GeoLocationEstimate(lat, lng, errAng);
			est.estimatedError = 0.5 * (est.haversine(new GeoLocation(lat + errAng, lng + errAng)) *
					est.haversine(new GeoLocation(lat - errAng, lng - errAng)));

			data.put(id, est);
		}
	}

	@Override
	public GeoLocationEstimate estimateLocation(QueryImageData query) {
		return data.get(query.flickrId);
	}
}
