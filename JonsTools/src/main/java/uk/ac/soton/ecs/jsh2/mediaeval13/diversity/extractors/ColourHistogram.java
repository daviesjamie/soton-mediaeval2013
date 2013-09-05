package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.extractors;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.image.pixel.statistics.HistogramModel;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;

public class ColourHistogram implements FeatureExtractor<DoubleFV, ResultItem> {

	@Override
	public DoubleFV extractFeature(ResultItem object) {
		final HistogramModel hm = new HistogramModel(4, 4, 4);
		hm.estimateModel(object.getMBFImage());
		return hm.histogram;
	}

}
