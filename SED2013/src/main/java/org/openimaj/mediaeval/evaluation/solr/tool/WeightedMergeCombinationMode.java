package org.openimaj.mediaeval.evaluation.solr.tool;

import org.openimaj.math.matrix.MatlibMatrixUtils;

import ch.akuhn.matrix.SparseMatrix;

/**
 * Given two sparse matricies, combine them.
 * This may be performed inplace using the first as the destination.
 * whether inplace or otherwise the combined matrix must be returned
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public enum WeightedMergeCombinationMode {
	SUM {
		@Override
		public SparseMatrix combine(SparseMatrix a, SparseMatrix b) {
			return MatlibMatrixUtils.plusInplace(a, b);
		}
	}, 
	MAX {
		@Override
		public SparseMatrix combine(SparseMatrix a, SparseMatrix b) {
			return MatlibMatrixUtils.maxInplace(a, b);
		}
	}, 
	MIN {
		@Override
		public SparseMatrix combine(SparseMatrix a, SparseMatrix b) {
			return MatlibMatrixUtils.minInplace(a, b);
		}
	}, 
	PRODUCT {
		@Override
		public SparseMatrix combine(SparseMatrix a, SparseMatrix b) {
			return MatlibMatrixUtils.timesInplace(a, b);
		}
	};
	
	public abstract SparseMatrix combine(SparseMatrix a, SparseMatrix b);
}
