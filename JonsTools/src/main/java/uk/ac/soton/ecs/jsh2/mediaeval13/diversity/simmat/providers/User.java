package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.providers;

import java.util.List;

import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.SimMatProvider;
import ch.akuhn.matrix.SparseMatrix;

public class User implements SimMatProvider {

	@Override
	public SparseMatrix computeSimilarityMatrix(List<ObjectDoublePair<ResultItem>> input) {
		final SparseMatrix sm = new SparseMatrix(input.size(), input.size());

		for (int i = 0; i < input.size(); i++) {
			sm.put(i, i, 1);
			final ResultItem ri = input.get(i).first;

			for (int j = i + 1; j < input.size(); j++) {
				final ResultItem rj = input.get(j).first;

				if (ri.username.equals(rj.username)) {
					sm.put(i, j, 1);
					sm.put(j, i, 1);
				}
			}
		}

		return sm;
	}
}
