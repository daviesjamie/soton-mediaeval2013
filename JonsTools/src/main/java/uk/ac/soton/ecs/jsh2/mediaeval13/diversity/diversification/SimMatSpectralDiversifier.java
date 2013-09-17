package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.knn.DoubleNearestNeighbours;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.knn.NearestNeighboursFactory;
import org.openimaj.ml.clustering.dbscan.DoubleNNDBSCAN;
import org.openimaj.ml.clustering.dbscan.SimilarityDBSCAN;
import org.openimaj.ml.clustering.dbscan.SparseMatrixDBSCAN;
import org.openimaj.ml.clustering.spectral.AbsoluteValueEigenChooser;
import org.openimaj.ml.clustering.spectral.DoubleSpectralClustering;
import org.openimaj.ml.clustering.spectral.SpectralClusteringConf;
import org.openimaj.util.array.ArrayUtils;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.SimMatProvider;
import ch.akuhn.matrix.SparseMatrix;

public class SimMatSpectralDiversifier implements Diversifier {
	private SimMatProvider provider;
	private int minPts;
	private double eps;
	private NearestNeighboursFactory<? extends DoubleNearestNeighbours, double[]> dist ;
	private double eiggap;
	private double eigsel;

	public SimMatSpectralDiversifier(
			SimMatProvider provider, 
			int minPts,
			double eps,
			DoubleFVComparison comp, double eiggap,
			double eigsel)
	{
		this.provider = provider;
		this.minPts = minPts;
		this.eps = eps;
		this.eiggap = eiggap;
		this.eigsel = eigsel;
		this.dist = new DoubleNearestNeighboursExact.Factory(comp);
	}

	@Override
	public List<ObjectDoublePair<ResultItem>> diversify(List<ObjectDoublePair<ResultItem>> input) {
		final SparseMatrix m = provider.computeSimilarityMatrix(input);

		DoubleNNDBSCAN internal = new DoubleNNDBSCAN(eps, minPts,dist );
		internal.setNoiseAsClusters(true);
		SpectralClusteringConf<double[]> conf = new SpectralClusteringConf<double[]>(internal);
		conf.eigenChooser = new AbsoluteValueEigenChooser(eiggap, eigsel);
		final DoubleSpectralClustering dsp = new DoubleSpectralClustering(conf);
		int[][] clusters = dsp.performClustering(m);	

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
