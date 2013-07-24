package org.openimaj.mediaeval.evaluation.cluster.processor;



import java.util.List;

import org.apache.log4j.Logger;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.math.matrix.MatrixUtils;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCAN;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;

import ch.akuhn.matrix.SparseMatrix;

/**
 * Wraps the functionality of a {@link DoubleDBSCAN} called with a sparse similarity matrix
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <T>
 */
public class SimilarityDoubleDBSCANWrapper<T> implements ClustererWrapper {
	Logger logger = Logger.getLogger(SimilarityDoubleDBSCANWrapper.class);
	private FeatureExtractor<DoubleFV, T> extractor;
	private DoubleDBSCAN dbscan;
	private List<T> data;
	/**
	 * @param extractor
	 * @param dbscan
	 *
	 */
	public SimilarityDoubleDBSCANWrapper(
			final List<T> data,FeatureExtractor<DoubleFV, T> extractor, DoubleDBSCAN dbscan
	) {
		this.data = data;
		this.extractor = extractor;
		this.dbscan = dbscan;
	}
	@Override
	public int[][] cluster() {
		logger.info(String.format("Constructing sparse matrix with %d features",data.size()));
		SparseMatrix mat = new SparseMatrix(data.size(), data.size());
		int i = 0;
		for (T ti : data) {
			int j = i;
			for (T tj : data.subList(j, data.size()) ) {
				double[] tiFeat = this.extractor.extractFeature(ti).values;
				double[] tjFeat = this.extractor.extractFeature(tj).values;
				double d = DoubleFVComparison.SUM_SQUARE.compare(tiFeat, tjFeat);
				if(d <= this.dbscan.getConfig().getEps()) {
					if(d==0 && i!=j)
						d=Double.MIN_VALUE;
					mat.put(i, j, d);
					mat.put(j, i, d);
				}

				j++;
			}
			i++;
		}
		logger.info(String.format("Similarity matrix sparcity: %2.5f",MatrixUtils.sparcity(mat)));
		DoubleDBSCANClusters res = dbscan.cluster(mat,true);
		return res.getClusterMembers();
	}

}
