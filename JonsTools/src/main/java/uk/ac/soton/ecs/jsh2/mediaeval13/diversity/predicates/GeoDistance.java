package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates;

import org.openimaj.util.function.Predicate;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;

/**
 * Filter based on the geolocations of the items in the resultset being within a
 * certain threshold of the known query location.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 */
public class GeoDistance implements Predicate<ResultItem> {
	private double maxDistKM = 1.0;
	private boolean acceptNoLocation = true;

	/**
	 * Construct
	 * 
	 * @param maxDistKM
	 *            the distance within items must be from the query
	 * @param acceptNoLocation
	 *            the value to return if an item doesn't have a geolocation
	 */
	public GeoDistance(double maxDistKM, boolean acceptNoLocation) {
		this.maxDistKM = maxDistKM;
		this.acceptNoLocation = acceptNoLocation;
	}

	@Override
	public boolean test(ResultItem object) {
		final GeoLocation gl = new GeoLocation(object.container.latitude, object.container.longitude);

		// query doesn't have a location (not possible?)
		if (gl.latitude == 0 && gl.longitude == 0)
			return true;

		final GeoLocation ql = new GeoLocation(object.latitude, object.longitude);

		// query doesn't have a location, so return acceptNoLocation
		if (ql.latitude == 0 && ql.longitude == 0)
			return acceptNoLocation;

		// distance less that threshold
		if (ql.haversine(gl) < maxDistKM)
			return true;

		return false;
	}
}
