package org.openimaj.mediaeval.evaluation.solr.tool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.openimaj.data.DoubleRange;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.knn.DoubleNearestNeighbours;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.knn.DoubleNearestNeighboursProvider;
import org.openimaj.knn.NearestNeighboursFactory;
import org.openimaj.ml.clustering.dbscan.DBSCANClusters;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;
import org.openimaj.ml.clustering.dbscan.DoubleNNDBSCAN;
import org.openimaj.ml.clustering.dbscan.SimilarityDBSCAN;
import org.openimaj.ml.clustering.spectral.SpectralClusteringConf;
import org.openimaj.util.pair.DoubleIntPair;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class NNDBSCANSetupMode extends SpatialClustererSetupMode{
	
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
	public double epsEnd = 0.8;
	
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
	
	@Option(
		name = "--eps",
		aliases = "-eps",
		required = false,
		usage = "List of eps",
		multiValued=true
	)
	private List<Double> eps = new ArrayList<Double>();
	
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
	public int minStart = 3;
	
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
	public int minEnd = 4;
	
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
	
	@Option(
		name = "--min-pts",
		aliases = "-mp",
		required = false,
		usage = "List of minpts",
		multiValued=true
	)
	private List<Double> minPts = new ArrayList<Double>();
	
	@Option(
		name = "--distance-metric",
		aliases = "-dist",
		required = false,
		usage = "The distance metric used by the DBSCAP"
	)
	DoubleFVComparison comp = DoubleFVComparison.EUCLIDEAN;


	private Iterator<Double> epsIter;
	private Iterator<Double> mpIter;

	private int currentMP;
	
	@Override
	public void setup() {
		epsIter= null;
		if(this.minPts.size() == 0)
			mpIter = new DoubleRange(minStart,minDelta,minEnd).iterator();
		else
			mpIter = minPts.iterator();
	}
	
	@Override
	public boolean hasNextSetup() {
		return (epsIter != null && epsIter.hasNext()) || mpIter.hasNext();
	}
	
	@Override
	public NamedSpecClusterConf nextClusterer() {
		NamedSpecClusterConf nc = new NamedSpecClusterConf();
		DoubleIntPair pp = nextEpsMinPair();
		DoubleNNDBSCAN internal = new DoubleNNDBSCAN(pp.first, pp.second, new DoubleNearestNeighboursExact.Factory(comp));
		internal.setNoiseAsClusters(true);
		nc.conf = new SpectralClusteringConf<double[]>(internal);
		nc.name = String.format("dist=%s/eps=%2.2f/minPts=%d",comp.name(),pp.first,pp.second);
		return nc;
	}

	private DoubleIntPair nextEpsMinPair() {
		if(epsIter == null || !epsIter.hasNext()){
			currentMP = (int)(double)mpIter.next();
			if(eps.size() == 0)
				epsIter = new DoubleRange(epsStart,epsDelta,epsEnd).iterator();
			else
				epsIter = eps.iterator();
		}
		
		return DoubleIntPair.pair(epsIter.next(), currentMP);
	}

}
