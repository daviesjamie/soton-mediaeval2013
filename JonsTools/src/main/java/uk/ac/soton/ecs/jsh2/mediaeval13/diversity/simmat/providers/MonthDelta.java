package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.providers;

import java.util.List;

import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.SimMatProvider;
import ch.akuhn.matrix.SparseMatrix;

public class MonthDelta implements SimMatProvider {
	double base;

	public MonthDelta(double base) {
		this.base = base;
	}

	@SuppressWarnings("deprecation")
	@Override
	public SparseMatrix computeSimilarityMatrix(List<ObjectDoublePair<ResultItem>> input) {
		final SparseMatrix sm = new SparseMatrix(input.size(), input.size());

		for (int i = 0; i < input.size(); i++) {
			sm.put(i, i, 1);
			final ResultItem ri = input.get(i).first;

			for (int j = i + 1; j < input.size(); j++) {
				final ResultItem rj = input.get(j).first;

				final int mi = ri.date_taken.getMonth();
				final int mj = rj.date_taken.getMonth();
				final int delta = mi > mj ? mi - mj : mj - mi;

				if (delta < base) {
					final double sim = delta == 0 ? 1 : 1 - Math.log(delta) / Math.log(base);
					if (sim > 0) {
						sm.put(i, j, sim);
						sm.put(j, i, sim);
					}
				}
			}
		}

		return sm;
	}

}
