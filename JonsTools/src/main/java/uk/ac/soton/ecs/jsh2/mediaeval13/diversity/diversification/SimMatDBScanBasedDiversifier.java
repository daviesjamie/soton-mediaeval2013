package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.ml.clustering.dbscan.SimilarityDBSCAN;
import org.openimaj.ml.clustering.dbscan.SparseMatrixDBSCAN;
import org.openimaj.util.array.ArrayUtils;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.SimMatProvider;
import ch.akuhn.matrix.SparseMatrix;

public class SimMatDBScanBasedDiversifier implements Diversifier {
	private SimMatProvider provider;
	private int minPts;
	private double eps;
	private boolean orderByAvgRelevance;

	public SimMatDBScanBasedDiversifier(SimMatProvider provider, int minPts,
			double eps, boolean orderByAvgRelevance)
	{
		this.provider = provider;
		this.minPts = minPts;
		this.eps = eps;
		this.orderByAvgRelevance = orderByAvgRelevance;
	}

	@Override
	public List<ObjectDoublePair<ResultItem>> diversify(List<ObjectDoublePair<ResultItem>> input) {
		final SparseMatrix m = provider.computeSimilarityMatrix(input);

		final SparseMatrixDBSCAN dbscan = new SimilarityDBSCAN(eps, minPts);

		final int[][] clusters = dbscan.cluster(m).clusters();

		if (orderByAvgRelevance) {
			final int[] sizes = new int[clusters.length];
			final double[] scores = new double[clusters.length];
			for (int i = 0; i < clusters.length; i++) {
				scores[i] = 0;
				for (int j = 0; j < clusters[i].length; j++) {
					scores[i] += input.get(clusters[i][j]).second;
				}
				sizes[i] = clusters[i].length;
				scores[i] /= clusters[i].length;
			}

			final int[][] oldClusters = clusters.clone();
			final int[] idx = ArrayUtils.indexSort(scores);
			for (int i = 0; i < oldClusters.length; i++) {
				clusters[oldClusters.length - i - 1] = oldClusters[idx[i]];
			}
		}

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
}
