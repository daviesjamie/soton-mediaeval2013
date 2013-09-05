package org.openimaj.mediaeval.evaluation.solr.tool;

import org.kohsuke.args4j.CmdLineOptionsProvider;

/**
 * The kind of experiments to run
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public enum SpatialClustererSetupOption implements CmdLineOptionsProvider{
	/**
	 * 
	 */
	DBSCAN {
		@Override
		public SpatialClustererSetupMode getOptions() {
			return new NNDBSCANSetupMode();
		}
	},
	/**
	 * 
	 */
	KMEANS{
		@Override
		public SpatialClustererSetupMode getOptions() {
			return new KMeansSetupMode();
		}
		
	};
	

	@Override
	public abstract SpatialClustererSetupMode getOptions() ;
	
}
