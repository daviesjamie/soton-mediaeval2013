package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat;

import java.util.List;

import ch.akuhn.matrix.SparseMatrix;

public interface SimMatCombiner {
	public SparseMatrix combine(List<SparseMatrix> matrices);
}
