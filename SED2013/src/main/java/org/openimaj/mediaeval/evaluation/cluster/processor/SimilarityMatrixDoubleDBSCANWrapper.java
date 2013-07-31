package org.openimaj.mediaeval.evaluation.cluster.processor;

import org.apache.log4j.Logger;
import org.openimaj.experiment.evaluation.cluster.processor.ClustererWrapper;
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
public class SimilarityMatrixDoubleDBSCANWrapper implements ClustererWrapper {
	Logger logger = Logger.getLogger(SimilarityMatrixDoubleDBSCANWrapper.class);

	private DoubleDBSCAN dbscan;
	private SparseMatrix mat;
	/**
	 * @param mat must be square
	 * @param dbscan
	 *
	 */
	public SimilarityMatrixDoubleDBSCANWrapper(SparseMatrix mat, DoubleDBSCAN dbscan
	) {
		this.mat = mat;
		this.dbscan = dbscan;
	}
	@Override
	public int[][] cluster() {
		logger.info(String.format("Similarity matrix sparcity: %2.5f",MatrixUtils.sparcity(mat)));
		DoubleDBSCANClusters res = dbscan.cluster(mat,true);
		return res.getClusterMembers();
	}

}
