package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat;

import java.util.List;

import ch.akuhn.matrix.SparseMatrix;

public class MinCombiner implements SimMatCombiner {

	@Override
	public SparseMatrix combine(List<SparseMatrix> matrices) {
		final int rows = matrices.get(0).rowCount();
		final int cols = matrices.get(0).columnCount();
		final SparseMatrix sm = new SparseMatrix(rows, cols);

		for (final SparseMatrix data : matrices) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					final double min = Math.min(sm.get(r, c), data.get(r, c));

					if (min != 0) {
						sm.put(r, c, min);
					}
				}
			}
		}

		return sm;
	}
}
