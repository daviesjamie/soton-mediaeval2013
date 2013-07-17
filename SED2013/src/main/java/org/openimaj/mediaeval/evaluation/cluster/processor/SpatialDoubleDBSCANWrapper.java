package org.openimaj.mediaeval.evaluation.cluster.processor;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.openimaj.data.DataSource;
import org.openimaj.data.RandomData;
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
public class SpatialDoubleDBSCANWrapper<T> implements ClustererWrapper<T> {
	Logger logger = Logger.getLogger(SpatialDoubleDBSCANWrapper.class);
	private final class ExtractedDatasource implements DataSource<double[]> {
		private final List<T> data;

		private ExtractedDatasource(List<T> data) {
			this.data = data;
		}

		@Override
		public Iterator<double[]> iterator() {
			final Iterator<T> dataIter = data.iterator();
			return new ExtractedIterator(dataIter);
		}

		@Override
		public void getData(int startRow, int stopRow, double[][] d) {
			List<T> subData = data.subList(startRow, stopRow);
			int i = 0;
			for (Iterator<double[]> iterator = new ExtractedIterator(subData.iterator()); iterator.hasNext();) {
				d[i++] = iterator.next();
			}
		}

		@Override
		public double[] getData(int row) {
			return extractor.extractFeature(data.get(row)).values;
		}

		@Override
		public int numDimensions() {
			return getData(0).length;
		}

		@Override
		public void getRandomRows(double[][] data) {
			int[] indexes = RandomData.getRandomIntArray(data.length, 0, numRows());
			for (int i = 0; i < indexes.length; i++) {
				data[i] = getData(indexes[i]);
			}
		}

		@Override
		public int numRows() {
			return data.size();
		}
	}
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
	public SpatialDoubleDBSCANWrapper(
			FeatureExtractor<DoubleFV, T> extractor, DoubleDBSCAN dbscan
	) {
		this.extractor = extractor;
		this.dbscan = dbscan;
	}
	@Override
	public int[][] cluster(final List<T> data) {
		DoubleDBSCANClusters res = dbscan.cluster(new ExtractedDatasource(data));
		return res.getClusterMembers();
	}

}
