package org.openimaj.util.data.dataset;

import org.openimaj.data.dataset.Dataset;

public interface DatasetBuilder<DATASET extends Dataset<?>> {
	public DATASET build(String[] args, int maxSize) throws BuildException;
}
