package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.util.Utils;

/**
 * Prior estimator based on a random sample of geo-coords
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class PriorEstimator implements GeoDensityEstimateProvider {
	private List<GeoLocation> prior;
	private TLongArrayList skipIds;
	private File latLngFile;

	public PriorEstimator(File latLngFile) {
		this.latLngFile = latLngFile;
	}

	@Override
	public void setSkipIds(TLongArrayList skipIds) {
		this.skipIds = skipIds;
	}

	@Override
	public void setSampleCount(int sampleCount) {
		try {
			prior = Utils.readLatLng(latLngFile, skipIds);
			Collections.shuffle(prior);
			prior = prior.subList(0, sampleCount);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<GeoLocation> estimatePoints(QueryImageData query) {
		return prior;
	}

	@Override
	public String toString() {
		return "PriorEstimator[file=" + latLngFile + "]";
	}
}
