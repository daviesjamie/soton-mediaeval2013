package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers;

import gnu.trove.list.array.TLongArrayList;

import java.util.List;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;

/**
 * Interface describing an object that can process a {@link QueryImageData} to
 * produce a collection of points representing likely geographic locations.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public interface GeoDensityEstimateProvider {
	/**
	 * Set the flickrIds that this estimator must ignore when computing the
	 * estimate
	 */
	void setSkipIds(TLongArrayList skipIds);

	/**
	 * Set the number of required points per feature
	 * 
	 * @param sampleCount
	 *            the number of points per feature
	 */
	void setSampleCount(int sampleCount);

	/**
	 * Estimate the density of geographical points for the given query. The
	 * returned list must either be empty (if no estimate can be made), or it
	 * must contain an integer multiple of sampleCount points. The multiple of
	 * the number of points is typically based on the number of features this
	 * {@link GeoDensityEstimateProvider} works with.
	 * 
	 * @param query
	 *            the query * @return the estimated location
	 */
	List<GeoLocation> estimatePoints(QueryImageData query);
}
