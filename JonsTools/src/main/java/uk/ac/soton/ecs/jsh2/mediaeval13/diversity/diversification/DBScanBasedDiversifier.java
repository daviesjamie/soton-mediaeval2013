package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparator;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.ml.clustering.dbscan.DistanceDBSCAN;
import org.openimaj.ml.clustering.dbscan.SimilarityDBSCAN;
import org.openimaj.ml.clustering.dbscan.SparseMatrixDBSCAN;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import ch.akuhn.matrix.SparseMatrix;

public class DBScanBasedDiversifier implements Diversifier {
	private DoubleFVComparator comparator;
	private FeatureExtractor<DoubleFV, ResultItem> extractor;
	private int minPts;
	private double eps;

	public DBScanBasedDiversifier(FeatureExtractor<DoubleFV, ResultItem> extr, DoubleFVComparator comp, int minPts,
			double eps)
	{
		this.comparator = comp;
		this.extractor = extr;
		this.minPts = minPts;
		this.eps = eps;
	}

	@Override
	public List<ObjectDoublePair<ResultItem>> diversify(List<ObjectDoublePair<ResultItem>> input) {
		final SparseMatrix m = buildMatrix(input);

		SparseMatrixDBSCAN dbscan;
		if (comparator.isDistance()) {
			dbscan = new DistanceDBSCAN(eps, minPts);
		} else {
			dbscan = new SimilarityDBSCAN(eps, minPts);
		}
		final int[][] clusters = dbscan.cluster(m).clusters();

		System.out.println(clusters);

		// sort by id so that higher rel items come first
		// for (int i = 0; i < clusters.length; i++) {
		// final int[] ids = clusters[i];
		// Arrays.sort(ids);
		// }
		// Arrays.sort(clusters, new Comparator<int[]>() {
		// @Override
		// public int compare(int[] o1, int[] o2) {
		// return o1.length == o2.length ? 0 : o1.length < o2.length ? -1 : 1;
		// }
		// });

		final List<ObjectDoublePair<ResultItem>> results = new ArrayList<ObjectDoublePair<ResultItem>>();
		for (int i = 0, cluster = 0, number = 0; i < Math.min(50, input.size()); i++) {
			if (number < clusters[cluster].length) {
				final int id = clusters[cluster][number];
				results.add(ObjectDoublePair.pair(input.get(id).first, 1.0 / (i + 1)));
			}

			cluster++;

			if (cluster >= clusters.length) {
				cluster = 0;
				number++;
			}
		}

		return results;
	}

	private SparseMatrix buildMatrix(List<ObjectDoublePair<ResultItem>> input) {
		final SparseMatrix m = new SparseMatrix(input.size(), input.size());

		final List<DoubleFV> features = new ArrayList<DoubleFV>();
		for (int i = 0; i < input.size(); i++)
			features.add(extractor.extractFeature(input.get(i).first));

		for (int i = 0; i < input.size(); i++) {
			for (int j = i; j < input.size(); j++) {
				final double val = comparator.compare(features.get(i), features.get(j));
				if (val != 0) {
					m.put(i, j, val);
					m.put(j, i, val);
				}
			}
		}

		return m;
	}
}
