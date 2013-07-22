package org.openimaj.tools.clustering;

import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.experiment.evaluation.classification.Classifier;

public interface ClassifierInstanceBuilder<DATA> {
	
	public Classifier<String, DATA> build(GroupedDataset<String, ListDataset<DATA>, DATA> trainingSource);
}
