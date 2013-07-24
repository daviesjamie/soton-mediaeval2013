package org.openimaj.tools.clustering;

import org.openimaj.data.dataset.Dataset;
import org.openimaj.feature.FeatureExtractor;

public interface FeatureExtractorBuilder<FEATURE, OBJECT> {

	public FeatureExtractor<FEATURE, OBJECT> build(Dataset<OBJECT> developmentSource);
	
}
