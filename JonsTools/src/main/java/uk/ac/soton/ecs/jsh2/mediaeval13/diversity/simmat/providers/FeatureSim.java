package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.providers;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparator;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.SimMatProvider;
import ch.akuhn.matrix.SparseMatrix;

public class FeatureSim implements SimMatProvider {
	FeatureExtractor<DoubleFV, ResultItem> extractor;
	DoubleFVComparator comp;

	/**
	 * @param extractor
	 * @param comp
	 */
	public FeatureSim(FeatureExtractor<DoubleFV, ResultItem> extractor, DoubleFVComparator comp) {
		this.extractor = extractor;
		this.comp = comp;
	}

	@Override
	public SparseMatrix computeSimilarityMatrix(List<ObjectDoublePair<ResultItem>> input) {
		final SparseMatrix sm = new SparseMatrix(input.size(), input.size());

		final List<DoubleFV> features = new ArrayList<DoubleFV>();
		for (int i = 0; i < input.size(); i++)
			features.add(extractor.extractFeature(input.get(i).first));

		for (int i = 0; i < input.size(); i++) {
			sm.put(i, i, 1);
			final DoubleFV hi = features.get(i);

			for (int j = i + 1; j < input.size(); j++) {
				final DoubleFV hj = features.get(j);

				final double sim = comp.compare(hi, hj);
				//System.out.println(sim);
				if (sim > 0) {
					sm.put(i, j, sim);
					sm.put(j, i, sim);
				}
			}
		}

		return sm;
	}
}
