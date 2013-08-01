package org.openimaj.mediaeval.feature.extractor;


import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;

import ch.akuhn.matrix.SparseMatrix;
import ch.akuhn.matrix.Vector;
import ch.akuhn.matrix.Vector.Entry;

/**
 * A similarity aggregator uses an underlying {@link FeatureExtractor} which
 * produces {@link SparseMatrix} similary matricies, aggregating them
 * to produce {@link DoubleFV} instances
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * @param <T>
 *
 */
public abstract class DatasetSimilarityAggregator<T> implements FeatureExtractor<DoubleFV, T> {

	private FeatureExtractor<SparseMatrix, T> ex;
	/**
	 * @param ex the extractor which produces similarity matricies
	 */
	public DatasetSimilarityAggregator(FeatureExtractor<SparseMatrix, T> ex) {
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
		DoubleFV fv = new DoubleFV(extractFeature.rowCount());
		for (int i = 0; i < fv.values.length; i++) {
			fv.values[i] = aggregate(extractFeature.row(i));
		}
		return fv;
	}

	/**
	 * @param row
	 * @return aggregates a row of similarity (i.e. the similarity to a specific document )
	 */
	public abstract double aggregate(Vector row) ;

	/**
	 * Produces the mean similarity, ignoring {@link Double#NaN} entries
	 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
	 *
	 * @param <T>
	 */
	public static class Mean<T> extends DatasetSimilarityAggregator<T>{

		/**
		 * @param ex the extractor
		 */
		public Mean(FeatureExtractor<SparseMatrix, T> ex) {
			super(ex);
		}

		@Override
		public double aggregate(Vector row) {
			double total = 0;
			int seen = 0;
			for (Entry vectorEntry : row.entries()) {
				double v = vectorEntry.value;
				if(!Double.isNaN(v)){
					total += v;
					seen ++;
				}
			}
			return total/seen;
		}

	}

}
