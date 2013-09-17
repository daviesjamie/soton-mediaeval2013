package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat;

import java.util.List;

import org.openimaj.math.matrix.MatlibMatrixUtils;
import org.openimaj.util.array.ArrayUtils;

import ch.akuhn.matrix.SparseMatrix;

public class AvgCombiner implements SimMatCombiner {

	private double[] weights;
	private double weightsSum;

	public AvgCombiner() {
		this.weights = null;
	}
	public AvgCombiner(double[] ds) {
		this.weights = ds;
		this.weightsSum = ArrayUtils.sumValues(ds);
	}

	@Override
	public SparseMatrix combine(List<SparseMatrix> matrices) {
		final int rows = matrices.get(0).rowCount();
		final int cols = matrices.get(0).columnCount();
		SparseMatrix sm = null;
		if(this.weights == null){			
			sm = unweightedAverage(matrices, rows, cols);
		}
		else{
			if(weights.length != matrices.size()){
				throw new RuntimeException("Not enough weights");
			}
			sm = weightedAverage(matrices,rows,cols);
			if(weightsSum==matrices.size()){				
				SparseMatrix unweightedSM = unweightedAverage(matrices, rows, cols);
				for (int i = 0; i < rows; i++) {
					for (int j = 0; j < cols; j++) {
						if(sm.get(i, j)!=unweightedSM.get(i, j)){
							throw new RuntimeException();
						}
					}
				}
			}
		}

		return sm;
	}
	private SparseMatrix weightedAverage(List<SparseMatrix> matrices, int rows,int cols) {
		SparseMatrix sm = new SparseMatrix(rows, cols);
		int i = 0;
		for (final SparseMatrix m : matrices) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					final double val = sm.get(r, c) + m.get(r, c) * weights[i]/weightsSum;

					if (val != 0) {
						sm.put(r, c, val);
					}
				}
			}
			i++;
		}
		return sm;
	}
	private SparseMatrix unweightedAverage(List<SparseMatrix> matrices, final int rows,
			final int cols) {
		SparseMatrix sm = new SparseMatrix(rows, cols);
		for (final SparseMatrix m : matrices) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					final double val = sm.get(r, c) + m.get(r, c);

					if (val != 0) {
						sm.put(r, c, val);
					}
				}
			}
		}

		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				final double val = sm.get(r, c) / matrices.size();

				if (val != 0) {
					sm.put(r, c, val);
				}
			}
		}
		return sm;
	}
}
