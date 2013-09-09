package org.openimaj.mediaeval.evaluation.solr.tool;

import org.openimaj.math.matrix.MatlibMatrixUtils;
import org.openimaj.mediaeval.evaluation.solr.SimilarityMatrixWrapper;

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
	/**
	 * The weighted mean 
	 */
	SUM {
		@Override
		public SimilarityMatrixWrapper combine(SimilarityMatrixWrapper[] tocombine, double[] curperm) {
//			return MatlibMatrixUtils.plusInplace(a, b);
			
			int nrows = tocombine[0].matrix().rowCount();
			int ncols = tocombine[0].matrix().columnCount();
			SparseMatrix denominator = new SparseMatrix(nrows, ncols);
			SparseMatrix numerator = new SparseMatrix(nrows, ncols);
			for (int r = 0; r < nrows; r++) {
				Vector denomRow = denominator.row(r);
				Vector numRow = numerator.row(r);
				for (int i = 0; i < tocombine.length; i++) {
					if(curperm[i] == 0) continue;
					Vector row = tocombine[i].matrix().row(r);
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
			return new SimilarityMatrixWrapper(out, tocombine[0]);
		}
	}, 
	/**
	 * max of the non zero weights 
	 */
	MAX {
		@Override
		public SimilarityMatrixWrapper combine(SimilarityMatrixWrapper[] tocombine, double[] curperm) {
			int nrows = tocombine[0].matrix().rowCount();
			int ncols = tocombine[0].matrix().columnCount();
			SparseMatrix out = new SparseMatrix(nrows, ncols);
			for (int r = 0; r < nrows; r++) {
				Vector outRow = out.row(r);
				for (int i = 0; i < tocombine.length; i++) {
					if(curperm[i] == 0) continue;
					Vector row = tocombine[i].matrix().row(r);
					for (Entry ent : row.entries()) {
						// This is where we actually do max
						if(ent.value > outRow.get(ent.index))
							outRow.put(ent.index, ent.value );
					}
				}
			}
			return new SimilarityMatrixWrapper(out, tocombine[0]);
		}
	}, 
	/**
	 * Min of the non zero weights 
	 */
	MIN {
		@Override
		public SimilarityMatrixWrapper combine(SimilarityMatrixWrapper[] tocombine, double[] curperm) {
			int nrows = tocombine[0].matrix().rowCount();
			int ncols = tocombine[0].matrix().columnCount();
			SparseMatrix out = new SparseMatrix(nrows, ncols);
			for (int r = 0; r < nrows; r++) {
				Vector outRow = out.row(r);
				for (int i = 0; i < tocombine.length; i++) {
					if(curperm[i] == 0) continue;
					Vector row = tocombine[i].matrix().row(r);
					for (Entry ent : row.entries()) {
						// This is where we actually do max
						if(ent.value < outRow.get(ent.index))
							outRow.put(ent.index, ent.value );
					}
				}
			}
			return new SimilarityMatrixWrapper(out, tocombine[0]);
		}
	}, 
	/**
	 * The weighted geometric mean
	 */
	PRODUCT {
		@Override
		public SimilarityMatrixWrapper combine(SimilarityMatrixWrapper[] tocombine, double[] curperm) {
//			return MatlibMatrixUtils.plusInplace(a, b);
			int nrows = tocombine[0].matrix().rowCount();
			int ncols = tocombine[0].matrix().columnCount();
			SparseMatrix denominator = new SparseMatrix(nrows, ncols);
			SparseMatrix numerator = new SparseMatrix(nrows, ncols);
			for (int r = 0; r < nrows; r++) {
				Vector denomRow = denominator.row(r);
				Vector numRow = numerator.row(r);
				for (int i = 0; i < tocombine.length; i++) {
					if(curperm[i] == 0) continue;
					Vector row = tocombine[i].matrix().row(r);
					for (Entry ent : row.entries()) {
						denomRow.add(ent.index, curperm[i]);
						numRow.put(ent.index, numRow.get(ent.index) * Math.pow(ent.value ,curperm[i]));
					}
				}
			}
			SparseMatrix out = new SparseMatrix(nrows, ncols);
			for (int r = 0; r < nrows; r++) {
				Vector denomRow = denominator.row(r);
				Vector numRow = numerator.row(r);
				Vector outRow = out.row(r);
				for (Entry ent : denomRow.entries()) {
					outRow.put(ent.index,Math.pow(numRow.get(ent.index) ,1./ent.value));
				}
			}
			return new SimilarityMatrixWrapper(out, tocombine[0]);
		}
	};
	
	/**
	 * @param tocombine
	 * @param weights
	 * @return combine the matricies of the {@link SimilarityMatrixWrapper} instances into a single {@link SimilarityMatrixWrapper}
	 */
	public abstract SimilarityMatrixWrapper combine(SimilarityMatrixWrapper[] tocombine, double[] weights);
}
