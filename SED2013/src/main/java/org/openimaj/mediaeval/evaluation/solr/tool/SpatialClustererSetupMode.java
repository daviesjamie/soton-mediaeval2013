package org.openimaj.mediaeval.evaluation.solr.tool;

import org.openimaj.ml.clustering.IndexClusters;
import org.openimaj.ml.clustering.SparseMatrixClusterer;
import org.openimaj.ml.clustering.spectral.SpectralClusteringConf;
import org.openimaj.ml.clustering.spectral.SpectralClusteringConf.ClustererProvider;

/**
 * The experiment mode
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public abstract class SpatialClustererSetupMode {
	protected class NamedSpecClusterConf{
		public String name;
		public SpectralClusteringConf<double[]> conf;
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
	public abstract NamedSpecClusterConf nextClusterer();
}
