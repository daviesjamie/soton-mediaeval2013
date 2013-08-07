package uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation;

/**
 * An estimated geolocation with an associated error in kilometers.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class GeoLocationEstimate extends GeoLocation {
	/**
	 * Error estimate for this geolocation in kilometers
	 */
	public double estimatedError;

	public GeoLocationEstimate(double lat, double lng, double estimate) {
		super(lat, lng);
		this.estimatedError = estimate;
	}
}
