package org.openimaj.mediaeval.evaluation.solr.tool;

import org.openimaj.math.matrix.MatlibMatrixUtils;

import ch.akuhn.matrix.SparseMatrix;
import ch.akuhn.matrix.Vector;
import ch.akuhn.matrix.Vector.Entry;

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
		public SparseMatrix combine(SparseMatrix[] tocombine, int[] curperm) {
//			return MatlibMatrixUtils.plusInplace(a, b);
			int nrows = tocombine[0].rowCount();
			int ncols = tocombine[0].columnCount();
			SparseMatrix denominator = new SparseMatrix(nrows, ncols);
			SparseMatrix numerator = new SparseMatrix(nrows, ncols);
			for (int r = 0; r < nrows; r++) {
				Vector denomRow = denominator.row(r);
				Vector numRow = numerator.row(r);
				for (int i = 0; i < tocombine.length; i++) {
					if(curperm[i] == 0) continue;
					Vector row = tocombine[i].row(r);
					for (Entry ent : row.entries()) {
						denomRow.add(ent.index, curperm[i]);
						numRow.add(ent.index, ent.value * curperm[i]);
					}
				}
			}
			SparseMatrix out = new SparseMatrix(nrows, ncols);
			for (int r = 0; r < nrows; r++) {
				Vector denomRow = denominator.row(r);
				Vector numRow = numerator.row(r);
				Vector outRow = out.row(r);
				for (Entry ent : denomRow.entries()) {
					outRow.put(ent.index,numRow.get(ent.index) / ent.value);
				}
			}
			return out;
		}
	}, 
	MAX {
		@Override
		public SparseMatrix combine(SparseMatrix[] tocombine, int[] curperm) {
//			return MatlibMatrixUtils.maxInplace(a, b);
			return null;
		}
	}, 
	MIN {
		@Override
		public SparseMatrix combine(SparseMatrix[] tocombine, int[] curperm) {
//			return MatlibMatrixUtils.minInplace(a, b);
			return null;
		}
	}, 
	PRODUCT {
		@Override
		public SparseMatrix combine(SparseMatrix[] tocombine, int[] curperm) {
//			return MatlibMatrixUtils.timesInplace(a, b);
			return null;
		}
	};
	
	public abstract SparseMatrix combine(SparseMatrix[] tocombine, int[] curperm);
}
