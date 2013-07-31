package org.openimaj.mediaeval.evaluation.cluster.processor;


import java.util.Iterator;

import org.apache.log4j.Logger;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.experiment.evaluation.cluster.processor.ClustererWrapper;
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
public class PrecachedSimilarityDoubleDBSCANWrapper<T> implements ClustererWrapper {
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
	private Dataset<T> data;
	/**
	 * @param data the data to be clustered
	 * @param extractor
	 * @param dbscan
	 *
	 */
	public PrecachedSimilarityDoubleDBSCANWrapper(Dataset<T> data, FeatureExtractor<DoubleFV, T> extractor, DoubleDBSCAN dbscan
	) {
		this.data = data;
		this.extractor = extractor;
		this.dbscan = dbscan;
	}
	@Override
	public int[][] cluster() {
		int numInstances = data.numInstances();
		SparseMatrix mat = new SparseMatrix(numInstances,numInstances);
		double[][] feats = new double[numInstances][];
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
				mat.put(i, j, d);
				mat.put(j, i, d);
			}
		}
		logger.info(String.format("Similarity matrix sparcity: %2.5f",MatrixUtils.sparcity(mat)));
		DoubleDBSCANClusters res = dbscan.cluster(mat,true);
		return res.getClusterMembers();
	}

}
