package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;
import org.openimaj.ml.clustering.dbscan.SimilarityDBSCAN;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import ch.akuhn.matrix.SparseMatrix;

public class TimeUserDiversifier implements Diversifier {
	double baseTime;
	double eps;

	public TimeUserDiversifier(double baseTime, double eps) {
		this.baseTime = baseTime;
		this.eps = eps;
	}

	@Override
	public List<ObjectDoublePair<ResultItem>> diversify(List<ObjectDoublePair<ResultItem>> input) {
		final SparseMatrix sm = new SparseMatrix(input.size(), input.size());

		for (int i = 0; i < input.size(); i++) {
			sm.put(i, i, 1);
			final ResultItem ri = input.get(i).first;

			for (int j = i + 1; j < input.size(); j++) {
				final ResultItem rj = input.get(j).first;

				if (ri.username.equals(rj.username)) {
					final double diff = Math.abs(ri.date_taken.getTime() - rj.date_taken.getTime()) / (1000.0 * 60.0);
					final double sim = diff == 0 ? 0 : 1 - Math.log(diff) / Math.log(baseTime);

					sm.put(i, j, sim);
					sm.put(j, i, sim);
				}
			}
		}

		final SimilarityDBSCAN dbscan = new SimilarityDBSCAN(eps, 1);
		dbscan.setNoiseAsClusters(true);
		final DoubleDBSCANClusters clstr = dbscan.cluster(sm);
		final int[][] clusters = clstr.clusters();

		// System.out.format("<h1>%s</h1>\n",
		// input.get(0).first.container.monument);
		// for (final int[] c : clusters) {
		// for (final int i : c) {
		// System.out.format("<img src=\"file://%s\" width='120'/>\n",
		// input.get(i).first.getImageFile());
		// }
		// System.out.println("<hr/>");
		// }

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
