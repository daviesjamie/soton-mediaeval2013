package org.openimaj.tools.clustering;

import org.openimaj.data.dataset.Dataset;

public interface DatasetBuilder<DatasetType extends Dataset> {
	public Dataset build(String[] args) throws BuildException;
}
