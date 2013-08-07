package uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation;

/**
 * Interface describing an object that guesses {@link GeoLocationEstimate}s for
 * a {@link QueryImage}.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public interface GeoPositioningEngine {
	GeoLocationEstimate estimateLocation(QueryImageData query);
}
