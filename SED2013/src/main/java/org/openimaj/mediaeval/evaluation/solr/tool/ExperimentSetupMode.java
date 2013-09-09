package org.openimaj.mediaeval.evaluation.solr.tool;

import org.openimaj.ml.clustering.IndexClusters;
import org.openimaj.ml.clustering.SparseMatrixClusterer;

/**
 * The experiment mode
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public abstract class ExperimentSetupMode {
	protected class NamedClusterer{
		public String name;
		public SparseMatrixClusterer<? extends IndexClusters> clusterer;
	}
	/**
	 * prepare the experimental setups
	 */
	public void setup(){
		
	}
	
	/**
	 * @return whether there is another experiment setup
	 */
	public abstract boolean hasNextSetup();
	
	/**
	 * @return the next clusterer, setup properly
	 * 
	 */
	public abstract NamedClusterer nextClusterer();
}
