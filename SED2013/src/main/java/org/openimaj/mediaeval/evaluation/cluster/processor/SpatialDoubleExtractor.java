package org.openimaj.mediaeval.evaluation.cluster.processor;

import java.util.List;

import org.apache.log4j.Logger;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.util.function.Function;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <T>
 */
public class SpatialDoubleExtractor<T> implements Function<List<T>,double[][]> {
	Logger logger = Logger.getLogger(SpatialDoubleExtractor.class);

	private FeatureExtractor<DoubleFV, T> extractor;
	/**
	 * @param extractor
	 *
	 */
	public SpatialDoubleExtractor(FeatureExtractor<DoubleFV, T> extractor) {
		this.extractor = extractor;
	}
	@Override
	public double[][] apply(List<T> data) {
		double[][] d = new double[data.size()][];
		int i = 0;
		for (T es : data) {
			double[] fv = this.extractor.extractFeature(es).values;
			for (int j = 0; j < fv.length; j++) {
				if(Double.isNaN(fv[j]))fv[j] = 0;
			}
			d[i++] = fv;
		}
		return d;
	}

}
