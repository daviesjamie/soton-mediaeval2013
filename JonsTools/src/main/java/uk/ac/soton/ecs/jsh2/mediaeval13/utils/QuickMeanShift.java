package uk.ac.soton.ecs.jsh2.mediaeval13.utils;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.feature.IntFV;
import org.openimaj.util.iterator.TextLineIterable;
import org.openimaj.util.pair.IndependentPair;
import org.openimaj.util.pair.IntDoublePair;
import org.openimaj.util.pair.ObjectIntPair;
import org.openimaj.util.tree.DoubleKDTree;

/**
 * Variant of Mean-Shift as found in scipy-learn. This performs a mean shift on
 * the data, possibly starting with external seed points, rather than using the
 * raw data. The final step assigns points to modes based on distance rather
 * than the underlying density, which is a bit wierd...
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class QuickMeanShift {
	public static interface Window {
		double[] window(double[] x, double[][] points, double bandwidth);
	}

	public static class FlatWindow implements Window {
		public static FlatWindow INSTANCE = new FlatWindow();

		@Override
		public double[] window(double[] x, double[][] points, double bandwidth) {
			final double[] v = new double[points[0].length];

			for (int j = 0; j < points.length; j++)
				for (int i = 0; i < v.length; i++)
					v[i] += points[j][i];

			for (int i = 0; i < v.length; i++)
				v[i] /= points.length;

			return v;
		}

	}

	public static IndependentPair<double[][], int[]>
			meanShift(double[][] X, double bandwidth, double[][] seeds, Window window, int max_iterations)
	{

		final int n_samples = X.length;
		final int n_features = X[0].length;
		final double stop_thresh = 1e-3 * bandwidth;
		final TObjectIntHashMap<DoubleFV> center_intensity_dict = new TObjectIntHashMap<DoubleFV>();

		DoubleKDTree tree = new DoubleKDTree(X, new DoubleKDTree.BBFMedianSplit());

		for (double[] my_mean : seeds) {
			for (int iter = 0; iter < max_iterations; iter++) {
				// Find mean of points within bandwidth
				final double[][] points_within = tree.coordinateRadiusSearch(my_mean, bandwidth);

				if (points_within.length == 0)
					break;

				final double[] my_old_mean = my_mean;
				my_mean = window.window(my_old_mean, points_within, bandwidth);

				if (DoubleFVComparison.EUCLIDEAN.compare(my_mean, my_old_mean) < stop_thresh) {
					center_intensity_dict.adjustOrPutValue(new DoubleFV(my_mean), points_within.length,
							points_within.length);
					break;
				}
			}
		}

		// # POST PROCESSING: remove near duplicate points
		// # If the distance between two kernels is less than the bandwidth,
		// # then we have to remove one because it is a duplicate. Remove the
		// # one with fewer points.
		final List<ObjectIntPair<DoubleFV>> sorted_by_intensity = new ArrayList<ObjectIntPair<DoubleFV>>();
		for (final DoubleFV fv : center_intensity_dict.keySet()) {
			sorted_by_intensity.add(ObjectIntPair.pair(fv, center_intensity_dict.get(fv)));
		}
		Collections.sort(sorted_by_intensity, ObjectIntPair.SECOND_ITEM_DESCENDING_COMPARATOR);

		final double[][] sorted_centers = new double[sorted_by_intensity.size()][];
		for (int i = 0; i < sorted_centers.length; i++) {
			sorted_centers[i] = sorted_by_intensity.get(i).first.values;
		}

		final boolean[] unique = new boolean[sorted_centers.length];
		Arrays.fill(unique, true);

		tree = new DoubleKDTree(sorted_centers, new DoubleKDTree.BBFMedianSplit());

		for (int i = 0; i < sorted_centers.length; i++) {
			final double[] center = sorted_centers[i];

			if (unique[i]) {
				final int[] neighbor_idxs = tree.indexRadiusSearch(center, bandwidth);
				for (final int j : neighbor_idxs) {
					unique[j] = false;
				}
				unique[i] = true;
			}
		}

		int count = 0;
		for (final boolean b : unique) {
			if (b)
				count++;
		}
		final double[][] cluster_centers = new double[count][];
		for (int i = 0, j = 0; i < unique.length; i++) {
			if (unique[i])
				cluster_centers[j++] = sorted_centers[i];
		}

		// ASSIGN LABELS: a point belongs to the cluster that it is closest to
		tree = new DoubleKDTree(cluster_centers, new DoubleKDTree.BBFMedianSplit());

		final int[] labels = new int[n_samples];
		for (int i = 0; i < n_samples; i++) {
			final double[] d = X[i];
			final IntDoublePair nn = tree.nearestNeighbour(d);
			labels[i] = nn.first;
		}

		return IndependentPair.pair(cluster_centers, labels);
	}

	public static double[][] bin_points(double[][] X, double bin_size, int min_bin_freq) {
		final TObjectIntHashMap<IntFV> bin_sizes = new TObjectIntHashMap<IntFV>();

		for (final double[] point : X) {
			final int[] binned_point = div(point, bin_size);
			bin_sizes.adjustOrPutValue(new IntFV(binned_point), 1, 1);
		}

		final List<double[]> bin_seeds = new ArrayList<double[]>();
		for (final IntFV fv : bin_sizes.keySet()) {
			if (bin_sizes.get(fv) > min_bin_freq) {
				final int[] ipts = fv.values;
				final double[] dpts = new double[ipts.length];

				for (int i = 0; i < ipts.length; i++)
					dpts[i] = ipts[i] * bin_size;

				bin_seeds.add(dpts);
			}
		}
		return bin_seeds.toArray(new double[bin_seeds.size()][]);
	}

	private static int[] div(double[] point, double bin_size) {
		final int[] out = new int[point.length];
		for (int i = 0; i < point.length; i++) {
			out[i] = (int) (point[i] / bin_size);
		}
		return out;
	}

	static double[][] readPoints(File f) {
		final List<double[]> pts = new ArrayList<double[]>();

		for (final String s : new TextLineIterable(f)) {
			final String[] parts = s.split(" ");
			pts.add(new double[] { Double.parseDouble(parts[0]), Double.parseDouble(parts[1]) });
		}

		return pts.toArray(new double[pts.size()][]);
	}

	public static void main(String[] args) {
		final double[][] pts = readPoints(new File("/Users/jon/Desktop/expt-geos-1000-prior/4767776.txt"));
		final double bw = 0.01;
		final double[][] seeds = bin_points(pts, bw, 1);
		final IndependentPair<double[][], int[]> result = meanShift(pts, bw, seeds, FlatWindow.INSTANCE, 300);

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

		double maxX = -1000;
		double minX = 1000;
		final double maxY = -1000;
		double minY = 1000;
		final double[][] biggest = new double[counts.get(maxIdx)][];
		for (int i = 0, j = 0; i < pts.length; i++) {
			if (result.secondObject()[i] == maxIdx) {
				biggest[j++] = pts[i];

				if (pts[i][0] > maxX)
					maxX = pts[i][0];
				if (pts[i][0] < minX)
					minX = pts[i][0];
				if (pts[i][1] > maxY)
					maxX = pts[i][1];
				if (pts[i][1] < minY)
					minY = pts[i][1];
			}
		}

		final double dx = maxX - minX;
		final double dy = maxY - minY;
		final double dist = Math.sqrt(dx * dx + dy * dy);

		System.out.println(Arrays.toString(result.firstObject()[maxIdx]) + " " + dist);
	}
}
