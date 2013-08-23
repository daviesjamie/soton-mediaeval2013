package org.openimaj.mediaeval.evaluation.solr.tool;

import org.openimaj.experiment.evaluation.cluster.processor.Clusterer;

import ch.akuhn.matrix.SparseMatrix;

/**
 * The experiment mode
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public abstract class ExperimentSetupMode {
	protected static class NamedClusterer{
		public String name;
		public Clusterer<SparseMatrix> clusterer;
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
