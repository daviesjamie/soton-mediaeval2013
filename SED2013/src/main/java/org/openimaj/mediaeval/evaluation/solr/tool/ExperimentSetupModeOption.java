package org.openimaj.mediaeval.evaluation.solr.tool;

import org.kohsuke.args4j.CmdLineOptionsProvider;

/**
 * The kind of experiments to run
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public enum ExperimentSetupModeOption implements CmdLineOptionsProvider{
	/**
	 * 
	 */
	DBSCAN {
		@Override
		public ExperimentSetupMode getOptions() {
			return new DBSCANSetupMode();
		}
	},
	/**
	 * 
	 */
	INCREMENTAL{
		@Override
		public ExperimentSetupMode getOptions() {
			return new IncrementalSetupMode();
		}
	},
	/**
	 * 
	 */
	LTINCREMENTAL{
		@Override
		public ExperimentSetupMode getOptions() {
			return new LTIncrementalSetupMode();
		}
	},
	/**
	 * 
	 */
	SPECTRAL{
		@Override
		public ExperimentSetupMode getOptions() {
			return new SpectralSetupMode();
		}
	};

	@Override
	public abstract ExperimentSetupMode getOptions() ;
	
}
