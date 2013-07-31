package org.openimaj.mediaeval.evaluation.cluster.processor;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.experiment.evaluation.cluster.processor.ClustererWrapper;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCAN;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;

/**
 * Wraps the functionality of a {@link DoubleDBSCAN} called spatially
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <T>
 */
public class SpatialDoubleDBSCANWrapper<T> implements ClustererWrapper {
	Logger logger = Logger.getLogger(SpatialDoubleDBSCANWrapper.class);

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
	 * @param extractor
	 * @param dbscan
	 *
	 */
	public SpatialDoubleDBSCANWrapper(
			final Dataset<T> data,FeatureExtractor<DoubleFV, T> extractor, DoubleDBSCAN dbscan
	) {
		this.data =data;
		this.extractor = extractor;
		this.dbscan = dbscan;
	}
	@Override
	public int[][] cluster() {
		double[][] d = new double[data.numInstances()][];
		int i = 0;
		for (T es : this.data) {
			d[i] = this.extractor.extractFeature(es).values;
		}
		DoubleDBSCANClusters res = dbscan.cluster(d);
		return res.getClusterMembers();
	}

}
