package org.openimaj.mediaeval.evaluation.cluster.processor;

import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrixFactoryMTJ;

import java.util.Iterator;
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
public class PrecachedSimilarityDoubleDBSCANWrapper<T> implements ClustererWrapper<T> {
	Logger logger = Logger.getLogger(PrecachedSimilarityDoubleDBSCANWrapper.class);

	private final class ExtractedIterator implements Iterator<double[]> {
		private final Iterator<T> dataIter;
		int seen = 0;

		private ExtractedIterator(Iterator<T> dataIter) {
			this.dataIter = dataIter;
		}

		@Override
		public void remove() { throw new UnsupportedOperationException();}

		@Override
		public double[] next() {
			if(seen++ % 1000 == 0){
				logger.info(String.format("Extracting feature for %dth image",seen-1));
			}
			return extractor.extractFeature(dataIter.next()).values;
		}

		@Override
		public boolean hasNext() {
			return dataIter.hasNext();
		}
	}
	private FeatureExtractor<DoubleFV, T> extractor;
	private DoubleDBSCAN dbscan;
	/**
	 * @param extractor
	 * @param dbscan
	 *
	 */
	public PrecachedSimilarityDoubleDBSCANWrapper(
			FeatureExtractor<DoubleFV, T> extractor, DoubleDBSCAN dbscan
	) {
		this.extractor = extractor;
		this.dbscan = dbscan;
	}
	@Override
	public int[][] cluster(final List<T> data) {
		SparseMatrix mat = SparseMatrixFactoryMTJ.INSTANCE.createMatrix(data.size(),data.size());
		double[][] feats = new double[data.size()][];
		int index = 0;
		for (Iterator<double[]> iterator = new ExtractedIterator(data.iterator()); iterator.hasNext();) {
			feats[index++] = iterator.next();
		}
		logger.info(String.format("Constructing sparse matrix with %d features",feats.length));
		for (int i = 0; i < feats.length; i++) {
			for (int j = i; j < feats.length; j++) {
				double d = DoubleFVComparison.SUM_SQUARE.compare(feats[i], feats[j]);
				if(d > this.dbscan.getConfig().getEps())
					continue;
				if(d==0 && i!=j)
					d=Double.MIN_VALUE;
				mat.setElement(i, j, d);
				mat.setElement(j, i, d);
			}
		}
		logger.info(String.format("Similarity matrix sparcity: %2.5f",CFMatrixUtils.sparcity(mat)));
		DoubleDBSCANClusters res = dbscan.cluster(mat,true);
		return res.getClusterMembers();
	}

}
