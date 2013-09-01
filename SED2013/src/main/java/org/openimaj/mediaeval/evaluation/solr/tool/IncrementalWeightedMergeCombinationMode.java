package org.openimaj.mediaeval.evaluation.solr.tool;

import org.openimaj.math.matrix.MatlibMatrixUtils;
import org.openimaj.mediaeval.evaluation.solr.SimilarityMatrixWrapper;

import ch.akuhn.matrix.SparseMatrix;

/**
 * Given two sparse matricies, combine them.
 * This may be performed inplace using the first as the destination.
 * whether inplace or otherwise the combined matrix must be returned
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * 
 *
 */
public enum IncrementalWeightedMergeCombinationMode {
	/**
	 * The weighted mean 
	 */
	SUM {

		@Override
		protected WeightedMergeCombinationMode equivalentMode() {
			return WeightedMergeCombinationMode.SUM;
		}
		
	}, 
	/**
	 * max of the non zero weights 
	 */
	MAX {
		@Override
		protected WeightedMergeCombinationMode equivalentMode() {
			return WeightedMergeCombinationMode.MAX;
		}
	}, 
	/**
	 * Min of the non zero weights 
	 */
	MIN {
		@Override
		protected WeightedMergeCombinationMode equivalentMode() {
			return WeightedMergeCombinationMode.MIN;
		}
	}, 
	/**
	 * The weighted geometric mean
	 */
	PRODUCT {
		@Override
		protected WeightedMergeCombinationMode equivalentMode() {
			return WeightedMergeCombinationMode.PRODUCT;
		}
	};
	
	/**
	 * Given the current matrix and next matrix along with their respective wieghts
	 * calculate the combination of the two
	 * @param current
	 * @param next
	 * @param currentweight
	 * @param nextweight
	 * @return the combine matricies using this mode's equivilant {@link WeightedMergeCombinationMode}. Might be null if both weights are 0.
	 */
	public SimilarityMatrixWrapper combine(SimilarityMatrixWrapper current, SimilarityMatrixWrapper next, double currentweight, double nextweight){
		if(nextweight == 0 && currentweight == 0){
			return null;
		}
		else if(nextweight == 0 && currentweight!=0){
			return current;
		}
		else if(nextweight != 0 && currentweight == 0){
			return next;
		}
		
		return equivalentMode().combine(new SimilarityMatrixWrapper[]{current,next}, new double[]{currentweight,nextweight});
	}

	protected abstract WeightedMergeCombinationMode equivalentMode() ;
}
