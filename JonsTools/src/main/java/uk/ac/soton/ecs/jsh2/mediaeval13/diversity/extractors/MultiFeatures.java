package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.extractors;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;

public class MultiFeatures implements FeatureExtractor<DoubleFV, ResultItem> {
	private FeatureExtractor<DoubleFV, ResultItem>[] extractors;

	public MultiFeatures(FeatureExtractor<DoubleFV, ResultItem>... extractors) {
		this.extractors = extractors;
	}

	@Override
	public DoubleFV extractFeature(ResultItem object) {
		DoubleFV fv = new DoubleFV(0);

		for (final FeatureExtractor<DoubleFV, ResultItem> fe : extractors) {
			fv = fv.concatenate(fe.extractFeature(object));
		}

		return fv;
	}
}
