package org.openimaj.mediaeval.evaluation.solr.tool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.openimaj.ml.clustering.IndexClusters;
import org.openimaj.ml.clustering.SparseMatrixClusterer;
import org.openimaj.ml.clustering.incremental.IncrementalLifetimeSparseClusterer;
import org.openimaj.ml.clustering.incremental.IncrementalSparseClusterer;

/**
 * 
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class LTIncrementalSetupMode extends IncrementalSetupMode{
	
	@Option(
		name = "--lifetime-limit",
		aliases = "-lt",
		required = false,
		usage = "",
		multiValued=true
	)
	private List<Integer> lifetimeLimit = new ArrayList<Integer>();
	private int currentLifetime = -1;
	private Iterator<Integer> ltIter;
	
	@Override
	public void setup() {
		if(lifetimeLimit.size() == 0){
			this.lifetimeLimit.add(3);
		}
		this.ltIter = lifetimeLimit.iterator();
	}
	
	@Override
	public boolean hasNextSetup() {
		return super.hasNextSetup() || this.ltIter.hasNext();
	}
	@Override
	public NamedClusterer nextClusterer() {
		if(!super.hasNextSetup() || this.currentLifetime == -1){
			this.currentLifetime = ltIter.next();
			super.setup();
		}
		NamedClusterer nc = super.nextClusterer();
		nc.name = String.format("lt=%d/%s",currentLifetime,nc.name);
		return nc;
	}
	@Override
	protected IncrementalSparseClusterer prepareIncrementalClusterer(SparseMatrixClusterer<? extends IndexClusters> clusterer) {
		return new IncrementalLifetimeSparseClusterer(
				clusterer,
				this.currentWindow,
				this.currentLifetime
		);
	}
}
