package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers;

import gnu.trove.list.array.TLongArrayList;

import java.util.ArrayList;
import java.util.List;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;

public class RandomEstimator implements GeoDensityEstimateProvider {

	protected int sampleCount;

	@Override
	public List<GeoLocation> estimatePoints(QueryImageData query) {
		List<GeoLocation> pts = new ArrayList<GeoLocation>(sampleCount);

		for( int i = 0; i < sampleCount; i++ ) {
			final double lat = (Math.random() * 180) - 90;
			final double lng = (Math.random() * 360) - 180;
			final double est = Math.random() * 5000;
			pts.add(new GeoLocationEstimate(lat, lng, est));
		}
		
		return pts;
	}

	@Override
	public void setSampleCount(int sampleCount) {
		this.sampleCount = sampleCount;
	}

	@Override
	public void setSkipIds(TLongArrayList skipIds) {
	}
}
