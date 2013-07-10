package org.openimaj.mediaeval.feature.extractor;

import gov.sandia.cognition.math.matrix.VectorEntry;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;
import gov.sandia.cognition.math.matrix.mtj.SparseVector;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;

/**
 * A similarity aggregator uses an underlying {@link FeatureExtractor} which
 * produces {@link SparseMatrix} similary matricies, aggregating them
 * to produce {@link DoubleFV} instances
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * @param <T>
 *
 */
public abstract class SimilarityAggregator<T> implements FeatureExtractor<DoubleFV, T> {

	private FeatureExtractor<SparseMatrix, T> ex;
	/**
	 * @param ex the extractor which produces similarity matricies
	 */
	public SimilarityAggregator(FeatureExtractor<SparseMatrix, T> ex) {
		this.ex = ex;
	}
	@Override
	public DoubleFV extractFeature(T object) {
		return aggregate(ex.extractFeature(object));
	}

	/**
	 * @param extractFeature
	 * @return aggregates the whole matrix by calling {@link #aggregate(SparseVector)} on each row
	 */
	public DoubleFV aggregate(SparseMatrix extractFeature) {
		DoubleFV fv = new DoubleFV(extractFeature.getNumRows());
		for (int i = 0; i < fv.values.length; i++) {
			fv.values[i] = aggregate(extractFeature.getRow(i));
		}
		return fv;
	}

	/**
	 * @param row
	 * @return aggregates a row of similarity (i.e. the similarity to a specific document )
	 */
	public abstract double aggregate(SparseVector row) ;

	/**
	 * Produces the mean similarity, ignoring {@link Double#NaN} entries
	 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
	 *
	 * @param <T>
	 */
	public static class Mean<T> extends SimilarityAggregator<T>{

		/**
		 * @param ex the extractor
		 */
		public Mean(FeatureExtractor<SparseMatrix, T> ex) {
			super(ex);
		}

		@Override
		public double aggregate(SparseVector row) {
			double total = 0;
			int seen = 0;
			for (VectorEntry vectorEntry : row) {
				double v = vectorEntry.getValue();
				if(!Double.isNaN(v)){
					total += v;
					seen ++;
				}
			}
			return total/seen;
		}

	}

}
