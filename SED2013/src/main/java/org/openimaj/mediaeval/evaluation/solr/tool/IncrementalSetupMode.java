package org.openimaj.mediaeval.evaluation.solr.tool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;
import org.openimaj.data.IntegerRange;
import org.openimaj.ml.clustering.IndexClusters;
import org.openimaj.ml.clustering.SparseMatrixClusterer;
import org.openimaj.ml.clustering.incremental.IncrementalSparseClusterer;

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
	
//	@Option(
//		name = "--maximum-window-size-prop",
//		aliases = "-maxwsprop",
//		required = false,
//		usage = ""
//	)
//	private double maxWinsizeProp = -1;
	
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
		currentWindow = -1;
		if(this.winSize.size() == 0){
			this.winSizeIter = new IntegerRange(winStart, winDelta, winEnd).iterator();
		}
		else{
			this.winSizeIter = winSize.iterator();
		}
	}
	@Override
	public boolean hasNextSetup() {
		boolean nextExp = experimentSetupModeOp.hasNextSetup();
		boolean nextWin = this.winSizeIter.hasNext();
		return nextExp || nextWin;
	}
	
	@Override
	public NamedClusterer nextClusterer() {
		if(!experimentSetupModeOp.hasNextSetup() || this.currentWindow == -1){
			experimentSetupModeOp.setup();
			this.currentWindow = this.winSizeIter.next();
		}
		NamedClusterer nextClusterer = experimentSetupModeOp.nextClusterer();
		
		nextClusterer.clusterer = prepareIncrementalClusterer(nextClusterer.clusterer);
		nextClusterer.name = String.format("window=%s/%s/%s",this.currentWindow,this.experimentSetupMode.name(),nextClusterer.name);
		return nextClusterer;
	}
	
	protected IncrementalSparseClusterer prepareIncrementalClusterer(SparseMatrixClusterer<? extends IndexClusters> clusterer) {
		return new IncrementalSparseClusterer(
			clusterer,
			this.currentWindow
//			,(int)(this.currentWindow * this.maxWinsizeProp)
		);
	}
	
	

}
