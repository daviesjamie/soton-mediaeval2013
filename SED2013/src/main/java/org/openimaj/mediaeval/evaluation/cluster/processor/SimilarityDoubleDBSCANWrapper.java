package org.openimaj.mediaeval.evaluation.cluster.processor;

import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrixFactoryMTJ;

import java.util.List;

import org.apache.log4j.Logger;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.math.matrix.CFMatrixUtils;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCAN;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;

/**
 * Wraps the functionality of a {@link DoubleDBSCAN} called with a sparse similarity matrix
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <T>
 */
public class SimilarityDoubleDBSCANWrapper<T> implements ClustererWrapper<T> {
	Logger logger = Logger.getLogger(SimilarityDoubleDBSCANWrapper.class);
	private FeatureExtractor<DoubleFV, T> extractor;
	private DoubleDBSCAN dbscan;
	/**
	 * @param extractor
	 * @param dbscan
	 *
	 */
	public SimilarityDoubleDBSCANWrapper(
			FeatureExtractor<DoubleFV, T> extractor, DoubleDBSCAN dbscan
	) {
		this.extractor = extractor;
		this.dbscan = dbscan;
	}
	@Override
	public int[][] cluster(final List<T> data) {
		logger.info(String.format("Constructing sparse matrix with %d features",data.size()));
		SparseMatrix mat = SparseMatrixFactoryMTJ.INSTANCE.createMatrix(data.size(), data.size());
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
					mat.setElement(i, j, d);
					mat.setElement(j, i, d);
				}

				j++;
			}
			i++;
		}
		logger.info(String.format("Similarity matrix sparcity: %2.5f",CFMatrixUtils.sparcity(mat)));
		DoubleDBSCANClusters res = dbscan.cluster(mat,true);
		return res.getClusterMembers();
	}

}
