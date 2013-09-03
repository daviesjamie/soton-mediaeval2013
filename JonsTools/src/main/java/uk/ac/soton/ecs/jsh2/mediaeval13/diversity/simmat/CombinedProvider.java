package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import ch.akuhn.matrix.SparseMatrix;

public class CombinedProvider implements SimMatProvider {
	SimMatProvider[] providers;
	SimMatCombiner combiner;

	public CombinedProvider(SimMatCombiner combiner, SimMatProvider... matProviders) {
		this.combiner = combiner;
		this.providers = matProviders;
	}

	@Override
	public SparseMatrix computeSimilarityMatrix(List<ObjectDoublePair<ResultItem>> results) {
		final List<SparseMatrix> mats = new ArrayList<SparseMatrix>();

		for (final SimMatProvider p : providers)
			mats.add(p.computeSimilarityMatrix(results));

		return combiner.combine(mats);
	}
}
