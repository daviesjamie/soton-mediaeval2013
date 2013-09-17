package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification;

import gnu.trove.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.SimMatProvider;
import ch.akuhn.matrix.SparseMatrix;

public class SimMatFoldingDiversifier implements Diversifier {
	private SimMatProvider provider;

	public SimMatFoldingDiversifier(SimMatProvider provider)
	{
		this.provider = provider;
	}

	@Override
	public List<ObjectDoublePair<ResultItem>> diversify(List<ObjectDoublePair<ResultItem>> input) {
		final TIntArrayList workingIds = new TIntArrayList();
		final List<TIntArrayList> resultIds = new ArrayList<TIntArrayList>();
		for (int i = 0; i < input.size(); i++)
			workingIds.add(i);

		final SparseMatrix sm = provider.computeSimilarityMatrix(input);

		final double avgSim = computeAvgSim(sm);

		resultIds.add(new TIntArrayList());
		resultIds.get(0).add(workingIds.remove(0));

		final List<ObjectDoublePair<ResultItem>> results = new ArrayList<ObjectDoublePair<ResultItem>>();
		results.add(ObjectDoublePair.pair(input.get(0).first, 1.0));

		int i = 1;
		while (resultIds.size() < 50 && workingIds.size() > 0) {
			final int rx = assign(resultIds, workingIds, sm, avgSim);

			if (rx == -1)
				break;

			workingIds.remove(workingIds.indexOf(rx));

			resultIds.add(new TIntArrayList());
			resultIds.get(resultIds.size() - 1).add(rx);
			results.add(ObjectDoublePair.pair(input.get(rx).first, 1.0 / (++i)));
		}

		return results;
	}

	private double computeAvgSim(SparseMatrix sm) {
		int n = 0;
		double v = 0;
		for (int r = 0; r < sm.rowCount(); r++) {
			for (int c = r + 1; c < sm.columnCount(); c++) {
				v += sm.get(r, c);
				n++;
			}
		}

		return v / n;
	}

	private int assign(List<TIntArrayList> resultIds, TIntArrayList working, SparseMatrix sm, double avgSim) {
		for (final int ri : working.toNativeArray()) {
			if (!assignToExisting(resultIds, ri, sm, avgSim))
				return ri;
			working.remove(working.indexOf(ri));
		}

		return -1;
	}

	private boolean assignToExisting(List<TIntArrayList> resultIds, int ri, SparseMatrix sm, double avgSim) {
		for (final TIntArrayList l : resultIds) {
			final int i = l.get(0);
			if (sm.get(ri, i) > avgSim) {
				l.add(ri);
				return true;
			}
		}
		return false;
	}
}
