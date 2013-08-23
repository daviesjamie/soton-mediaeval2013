package org.openimaj.mediaeval.evaluation.solr.tool;

import java.util.Iterator;

import org.kohsuke.args4j.Option;
import org.openimaj.data.DoubleRange;
import org.openimaj.ml.clustering.dbscan.SimilarityDBSCAN;
import org.openimaj.util.pair.DoubleIntPair;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class DBSCANSetupMode extends ExperimentSetupMode {
	
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--eps-start", 
		aliases="-epss", 
		required=false, 
		usage="The epsilon value of DBSCAN.", 
		metaVar="DOUBLE"
	)
	public double epsStart = 0.4;
	
	/**
	 * The end eps to searc
	 */
	@Option(
		name="--eps-end", 
		aliases="-epse", 
		required=false, 
		usage="The end epsilon value of DBSCAN.", 
		metaVar="DOUBLE"
	)
	public double epsEnd = 1.0;
	
	/**
	 * The delta of eps to search
	 */
	@Option(
		name="--eps-delta", 
		aliases="-epsd", 
		required=false, 
		usage="The epsilon delta.", 
		metaVar="DOUBLE"
	)
	public double epsDelta= 0.05;
	
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--min-pts-start", 
		aliases="-mps", 
		required=false, 
		usage="The minpts for DBSCAN", 
		metaVar="INTEGER"
	)
	public int minStart = 1;
	
	/**
	 * The end eps to searc
	 */
	@Option(
		name="--min-pts-end", 
		aliases="-mpe", 
		required=false, 
		usage="The end minpts for DBSCAN", 
		metaVar="INTEGER"
	)
	public int minEnd = 5;
	
	/**
	 * The delta of eps to search
	 */
	@Option(
		name="--min-pts-delta", 
		aliases="-mpd", 
		required=false, 
		usage="The epsilon delta.", 
		metaVar="DOUBLE"
	)
	public int minDelta= 1;


	private Iterator<Double> epsIter;
	private Iterator<Double> mpIter;

	private int currentMP;
	
	@Override
	public void setup() {
		epsIter= null;
		mpIter = new DoubleRange(minStart,minDelta,minEnd).iterator();
	}
	
	@Override
	public boolean hasNextSetup() {
		return (epsIter != null && epsIter.hasNext()) || mpIter.hasNext();
	}

	@Override
	public NamedClusterer nextClusterer() {
		NamedClusterer nc = new NamedClusterer();
		DoubleIntPair pp = nextEpsMinPair();
		nc.clusterer = new SimilarityDBSCAN(pp.first,pp.second);
		nc.name = String.format("eps=%2.2f,minPts=%d",pp.first,pp.second);
		return nc;
	}

	private DoubleIntPair nextEpsMinPair() {
		if(epsIter == null || !epsIter.hasNext()){
			currentMP = (int)(double)mpIter.next();
			epsIter = new DoubleRange(epsStart,epsDelta,epsEnd).iterator();
		}
		
		return DoubleIntPair.pair(epsIter.next(), currentMP);
	}

}
