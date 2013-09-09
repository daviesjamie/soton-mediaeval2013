package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.openimaj.data.RandomData;
import org.openimaj.util.list.AcceptingListView;

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
	private int sampleCount;

	public PriorEstimator(File latLngFile) {
		this.latLngFile = latLngFile;
	}

	@Override
	public void setSkipIds(TLongArrayList skipIds) {
		this.skipIds = skipIds;

		try {
			prior = Utils.readLatLng(latLngFile, skipIds);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setSampleCount(int sampleCount) {
		this.sampleCount = sampleCount;
	}

	@Override
	public List<GeoLocation> estimatePoints(QueryImageData query) {
		return new AcceptingListView<GeoLocation>(prior, RandomData.getUniqueRandomInts(sampleCount, 0, prior.size()));
	}

	@Override
	public String toString() {
		return "PriorEstimator[file=" + latLngFile + "]";
	}
}
