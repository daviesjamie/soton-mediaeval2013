package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat;

import java.util.List;

import ch.akuhn.matrix.SparseMatrix;

public class AvgCombiner implements SimMatCombiner {

	@Override
	public SparseMatrix combine(List<SparseMatrix> matrices) {
		final int rows = matrices.get(0).rowCount();
		final int cols = matrices.get(0).columnCount();
		final SparseMatrix sm = new SparseMatrix(rows, cols);

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
