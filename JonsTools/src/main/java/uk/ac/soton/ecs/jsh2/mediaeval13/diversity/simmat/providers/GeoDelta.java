package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.providers;

import java.util.List;

import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.SimMatProvider;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import ch.akuhn.matrix.SparseMatrix;

/**
 * Geo based similarity. Sim is 1 - dist/base iff dist<base else 0.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class GeoDelta implements SimMatProvider {
	double baseDistance;

	public GeoDelta(double baseDistance) {
		this.baseDistance = baseDistance;
	}

	@Override
	public SparseMatrix computeSimilarityMatrix(List<ObjectDoublePair<ResultItem>> input) {
		final SparseMatrix sm = new SparseMatrix(input.size(), input.size());

		for (int i = 0; i < input.size(); i++) {
			sm.put(i, i, 1);
			final ResultItem ri = input.get(i).first;
			final GeoLocation gi = new GeoLocation(ri.latitude, ri.longitude);

			if (gi.latitude == 0 && gi.longitude == 0)
				continue;

			for (int j = i + 1; j < input.size(); j++) {
				final ResultItem rj = input.get(j).first;
				final GeoLocation gj = new GeoLocation(rj.latitude, rj.longitude);

				if (gj.latitude == 0 && gj.longitude == 0)
					continue;

				final double diff = gi.haversine(gj);
				final double sim = 1 - diff / baseDistance;
				if (sim <= 0)
					continue;

				sm.put(i, j, sim);
				sm.put(j, i, sim);
			}
		}

		return sm;
	}
}
