package org.openimaj.tools.features.featureextractor;

import org.openimaj.data.dataset.Dataset;
import org.openimaj.feature.FeatureExtractor;

public interface FeatureExtractorInstanceBuilder<FEATURE, OBJECT> {

	public FeatureExtractor<FEATURE, OBJECT> build(Dataset<OBJECT> developmentSource);
	
}
