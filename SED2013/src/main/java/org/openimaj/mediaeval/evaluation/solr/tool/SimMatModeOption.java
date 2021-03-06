package org.openimaj.mediaeval.evaluation.solr.tool;

import org.kohsuke.args4j.CmdLineOptionsProvider;

/**
 * How sets of simmat matricies are handled
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public enum SimMatModeOption implements CmdLineOptionsProvider{
	/**
	 * 
	 */
	OAAT{

		@Override
		public SimMatSetupMode getOptions() {
			return new OneAtATimeSimMatMode();
		}
		
	},
	/**
	 * 
	 */
	WEIGHTEDMERGE{

		@Override
		public SimMatSetupMode getOptions() {
			return new WeightedMergeSimMatMode();
		}
		
	},
	/**
	 * 
	 */
	IWEIGHTEDMERGE{

		@Override
		public SimMatSetupMode getOptions() {
			return new IncrementalWeightedMergeMode();
		}
		
	},
	/**
	 * 
	 */
	METRICIWM{

		@Override
		public SimMatSetupMode getOptions() {
			return new MetricSpaceIWMMode();
		}
		
	},
	;

	@Override
	public abstract SimMatSetupMode getOptions() ;
	
}
