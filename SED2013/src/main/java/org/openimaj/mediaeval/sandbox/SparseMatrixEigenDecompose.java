package org.openimaj.mediaeval.sandbox;

import java.util.Random;

import org.openimaj.math.matrix.MatrixUtils;
import org.openimaj.time.Timer;

import ch.akuhn.matrix.SparseMatrix;
import ch.akuhn.matrix.Vector;
import ch.akuhn.matrix.eigenvalues.Eigenvalues;
import ch.akuhn.matrix.eigenvalues.FewEigenvalues;

public class SparseMatrixEigenDecompose {
	public static void main(String[] args) {
		int nm = 300000;
		Timer t = Timer.timer();
		final SparseMatrix m = randomSparseMatrix(nm, nm, 0.00001);
//		SparseMatrix m = SparseMatrix.random(nm, nm, 0.00001);
		System.out.println(
			String.format(
				"Matrix Created, Size: %dx%d, Density: %2.6f, Took: %2.5fs",
				m.rowCount(),m.columnCount(),MatrixUtils.sparcity(m), t.duration()/1000.
			)
		);
		
		FewEigenvalues eig = new FewEigenvalues(m.columnCount()) {
			@Override
			protected Vector callback(Vector vector) {
				return m.mult(vector);
			}
		}.greatest(12);
		System.out.println(String.format("Running eig"));
		Eigenvalues ev = eig.run();
		System.out.println(String.format("Took: %2.5fs",t.duration()/1000.));
	}

	private static SparseMatrix randomSparseMatrix(int n, int m, double d) {
		long nm = (long) ((long)n * (long)m * d);
		SparseMatrix sm = new SparseMatrix(n,m);
		Random r = new Random();
		for (long i = 0; i < nm; i++) {
			sm.put(r.nextInt(n), r.nextInt(m), r.nextDouble());
		}
		return sm;
	}
}
