package org.openimaj.tools.classification.classifierbuilder;

import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.experiment.evaluation.classification.Classifier;
import org.openimaj.feature.FeatureExtractor;

public interface ClassifierInstanceBuilder<FEATURE, DATA> {
	
	public Classifier<String, DATA> build(GroupedDataset<String, ListDataset<DATA>, DATA> trainingSource,
										  FeatureExtractor<FEATURE, DATA> featureExtractor);
}
