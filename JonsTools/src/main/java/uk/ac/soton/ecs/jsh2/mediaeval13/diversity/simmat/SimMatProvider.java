package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat;

import java.util.List;

import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import ch.akuhn.matrix.SparseMatrix;

public interface SimMatProvider {
	public SparseMatrix computeSimilarityMatrix(List<ObjectDoublePair<ResultItem>> results);
}
