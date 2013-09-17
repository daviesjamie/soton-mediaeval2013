package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift;

import gnu.trove.map.hash.TIntIntHashMap;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.openimaj.util.pair.IndependentPair;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoPositioningEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.GeoDensityEstimateProvider;
import uk.ac.soton.ecs.jsh2.mediaeval13.utils.QuickMeanShift;
import uk.ac.soton.ecs.jsh2.mediaeval13.utils.QuickMeanShift.FlatWindow;

/**
 * Extensible Mean-Shift based placing algorithm using {@link QuickMeanShift}
 * together with a configurable set of "features" provided by
 * {@link GeoDensityEstimateProvider}s.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 */
public class ExtensibleMeanShiftEngine implements GeoPositioningEngine {
	private static final int MIN_BIN_FREQ = 1;
	private static final int MAX_ITERATIONS = 300;

	private static final double UNKNOWN_DISTANCE = 5800;
	private static final double UNKNOWN_LAT = 90;
	private static final double UNKNOWN_LNG = 0;

	protected double bandwidth;
	protected GeoDensityEstimateProvider[] providers;

	public ExtensibleMeanShiftEngine(int sampleCount, double bandwidth, GeoDensityEstimateProvider... providers) {
		this.bandwidth = bandwidth;
		this.providers = providers;

		for (final GeoDensityEstimateProvider p : providers)
			p.setSampleCount(sampleCount);
	}

	@Override
	public GeoLocationEstimate estimateLocation(QueryImageData query) {
		final List<GeoLocation> pts = new ArrayList<GeoLocation>();

		for (final GeoDensityEstimateProvider p : providers)
			pts.addAll(p.estimatePoints(query));

		if (pts.size() == 0)
			return new GeoLocationEstimate(UNKNOWN_LAT, UNKNOWN_LNG, UNKNOWN_DISTANCE);
		if (allSame(pts))
			return new GeoLocationEstimate(pts.get(0).latitude, pts.get(0).longitude, 0);

		// for (final GeoLocation gl : pts) {
		// System.out.println(gl.longitude + "," + gl.latitude);
		// }

		final GeoLocationEstimate estimate = computeLocationMeanShift(pts);

		return estimate;
	}

	private boolean allSame(List<GeoLocation> pts) {
		final GeoLocation first = pts.get(0);
		for (int i = 1; i < pts.size(); i++) {
			final GeoLocation pt = pts.get(i);
			if (first.latitude != pt.latitude || first.longitude != pt.longitude)
				return false;
		}
		return true;
	}

	protected GeoLocationEstimate computeLocationMeanShift(final List<GeoLocation> pts) {
		final double[][] data = toArray(pts);

		final double[][] seeds = QuickMeanShift.bin_points(data, bandwidth / 2, MIN_BIN_FREQ);
		final IndependentPair<double[][], int[]> result = QuickMeanShift.meanShift(data, bandwidth, seeds,
				FlatWindow.INSTANCE, MAX_ITERATIONS);

		final TIntIntHashMap counts = new TIntIntHashMap();
		for (final int i : result.secondObject())
			counts.adjustOrPutValue(i, 1, 1);

		int maxIdx = -1;
		int maxValue = 0;
		for (final int idx : counts.keys()) {
			final int count = counts.get(idx);
			if (count > maxValue) {
				maxValue = count;
				maxIdx = idx;
			}
		}

		// double maxX = -1000;
		// double minX = 1000;
		// double maxY = -1000;
		// double minY = 1000;
		// final double[][] biggest = new double[counts.get(maxIdx)][];
		// for (int i = 0, j = 0; i < data.length; i++) {
		// if (result.secondObject()[i] == maxIdx) {
		// biggest[j++] = data[i];
		//
		// if (data[i][0] > maxX)
		// maxX = data[i][0];
		// if (data[i][0] < minX)
		// minX = data[i][0];
		// if (data[i][1] > maxY)
		// maxY = data[i][1];
		// if (data[i][1] < minY)
		// minY = data[i][1];
		// }
		// }
		//
		// final double dx = maxX - minX;
		// final double dy = maxY - minY;
		// final double varKM = Math.sqrt((dx * dx) + (dy * dy));

		final double lat = result.firstObject()[maxIdx][1];
		final double lng = result.firstObject()[maxIdx][0];

		final DescriptiveStatistics stats = new DescriptiveStatistics();
		for (int i = 0; i < data.length; i++) {
			if (result.secondObject()[i] == maxIdx) {
				final double thislat = data[i][1];
				final double thislng = data[i][0];

				final double dlat = lat - thislat;
				final double dlng = lng - thislng;
				final double dist = Math.sqrt((dlat * dlat) + (dlng * dlng));
				stats.addValue(dist);
			}
		}

		final double varDeg = stats.getVariance();
		final double varKM = new GeoLocation(lat, lng).haversine(new GeoLocation(lat - varDeg, lng));

		return new GeoLocationEstimate(lat, lng, varKM);
	}

	private double[][] toArray(List<GeoLocation> pts) {
		final double[][] data = new double[pts.size()][];

		for (int i = 0; i < data.length; i++) {
			data[i] = new double[] { pts.get(i).longitude, pts.get(i).latitude };
		}

		return data;
	}
}
