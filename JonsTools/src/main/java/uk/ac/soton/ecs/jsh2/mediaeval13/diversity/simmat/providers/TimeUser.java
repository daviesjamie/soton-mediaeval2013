package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.providers;

import java.util.List;

import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.SimMatProvider;
import ch.akuhn.matrix.SparseMatrix;

/**
 * Time-user based similarity. Users must be the same to have any sim; time sim
 * is log(delta)/log(base), so after delta==base, sim is 0 and if delta==0, sim
 * is 1
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class TimeUser implements SimMatProvider {
	double baseTime;

	public TimeUser(double baseTime) {
		this.baseTime = baseTime;
	}

	@Override
	public SparseMatrix computeSimilarityMatrix(List<ObjectDoublePair<ResultItem>> input) {
		final SparseMatrix sm = new SparseMatrix(input.size(), input.size());

		for (int i = 0; i < input.size(); i++) {
			sm.put(i, i, 1);
			final ResultItem ri = input.get(i).first;

			for (int j = i + 1; j < input.size(); j++) {
				final ResultItem rj = input.get(j).first;

				if (ri.username.equals(rj.username)) {
					final double diff = Math.abs(ri.date_taken.getTime() - rj.date_taken.getTime()) / (1000.0 * 60);
					double sim = diff <= 1 ? 1 : 1 - Math.log(diff) / Math.log(baseTime);
					sim = sim < 0 ? 0 : sim;

					sm.put(i, j, sim);
					sm.put(j, i, sim);
				}
			}
		}

		return sm;
	}
}
