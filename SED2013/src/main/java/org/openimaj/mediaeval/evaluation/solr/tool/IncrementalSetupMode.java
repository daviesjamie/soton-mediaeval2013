package org.openimaj.mediaeval.evaluation.solr.tool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;
import org.openimaj.data.DoubleRange;
import org.openimaj.data.IntegerRange;
import org.openimaj.ml.clustering.IndexClusters;
import org.openimaj.ml.clustering.SparseMatrixClusterer;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;
import org.openimaj.ml.clustering.dbscan.SimilarityDBSCAN;
import org.openimaj.ml.clustering.incremental.IncrementalSparseClusterer;
import org.openimaj.util.pair.DoubleIntPair;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class IncrementalSetupMode extends ExperimentSetupMode {
	
	
	/**
	 * The win to start to search
	 */
	@Option(
		name="--window-size-start", 
		aliases="-wss", 
		required=false, 
		usage="Window size start", 
		metaVar="INTEGER"
	)
	public int winStart = 100;
	
	/**
	 * The win to end to search
	 */
	@Option(
		name="--window-size-end", 
		aliases="-wse", 
		required=false, 
		usage="Window size end", 
		metaVar="INTEGER"
	)
	public int winEnd = 1000;	
	/**
	 * The delta of eps to search
	 */
	@Option(
		name="--window-size-delta", 
		aliases="-wsd", 
		required=false, 
		usage="The window size delta.", 
		metaVar="INTEGER"
	)
	public int winDelta = 100;
	
	@Option(
		name = "--window-size",
		aliases = "-ws",
		required = false,
		usage = "",
		multiValued=true
	)
	private List<Integer> winSize = new ArrayList<Integer>();
	
	@Option(
		name = "--incremental-experiment-mode",
		aliases = "-iem",
		required = true,
		usage = "The experiment mode to run incrementally",
		handler = ProxyOptionHandler.class
	)
	ExperimentSetupModeOption experimentSetupMode = null;
	ExperimentSetupMode experimentSetupModeOp = null;

	private Iterator<Integer> winSizeIter;
	
	int currentWindow = -1;
	@Override
	public void setup() {
		this.experimentSetupModeOp.setup();
		if(this.winSize.size() == 0){
			this.winSizeIter = new IntegerRange(winStart, winDelta, winEnd).iterator();
		}
		else{
			this.winSizeIter = winSize.iterator();
		}
	}
	@Override
	public boolean hasNextSetup() {
		return experimentSetupModeOp.hasNextSetup() || this.winSizeIter.hasNext();
	}
	
	@Override
	public NamedClusterer nextClusterer() {
		if(!experimentSetupModeOp.hasNextSetup() || this.currentWindow == -1){
			experimentSetupModeOp.setup();
			this.currentWindow = this.winSizeIter.next();
		}
		NamedClusterer nextClusterer = experimentSetupModeOp.nextClusterer();
		
		nextClusterer.clusterer = new IncrementalSparseClusterer(
			nextClusterer.clusterer, 
			this.currentWindow
		);
		nextClusterer.name = String.format("window=%s/%s/%s",this.currentWindow,this.experimentSetupMode.name(),nextClusterer.name);
		return nextClusterer;
	}
	
	

}
