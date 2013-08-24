package org.openimaj.mediaeval.evaluation.solr.tool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;
import org.openimaj.data.DoubleRange;
import org.openimaj.data.IntegerRange;
import org.openimaj.mediaeval.evaluation.solr.tool.ExperimentSetupMode.NamedClusterer;
import org.openimaj.mediaeval.evaluation.solr.tool.SpatialClustererSetupMode.NamedSpecClusterConf;
import org.openimaj.ml.clustering.dbscan.DBSCANClusters;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;
import org.openimaj.ml.clustering.dbscan.SimilarityDBSCAN;
import org.openimaj.ml.clustering.incremental.IncrementalSparseClusterer;
import org.openimaj.ml.clustering.spectral.AbsoluteValueEigenChooser;
import org.openimaj.ml.clustering.spectral.DoubleSpectralClustering;
import org.openimaj.util.pair.DoubleIntPair;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SpectralSetupMode extends ExperimentSetupMode{
	
	/**
	 * The absolute eigen value change 
	 */
	@Option(
		name="--eig-start", 
		aliases="-eigs", 
		required=false, 
		usage="The eigenvalue proportion change used to choose eigen vectors. Start", 
		metaVar="DOUBLE"
	)
	public double eigStart = 0.2;
	
	/**
	 * The end eps to searc
	 */
	@Option(
		name="--eig-end", 
		aliases="-eige", 
		required=false, 
		usage="The eigenvalue proportion change used to choose eigen vectors. End", 
		metaVar="DOUBLE"
	)
	public double eigEnd = 0.9;
	
	/**
	 * The delta of eps to search
	 */
	@Option(
		name="--eig-delta", 
		aliases="-eigd", 
		required=false, 
		usage="The eigenvalue proportion change used to choose eigen vectors. Delta", 
		metaVar="DOUBLE"
	)
	public double eigDelta= 0.4;
	
	@Option(
		name = "--eig",
		aliases = "-eig",
		required = false,
		usage = "List of eig props to search",
		multiValued=true
	)
	private List<Double> eig = new ArrayList<Double>();
	
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--eig-select", 
		aliases="-eigsel", 
		required=false, 
		usage="The number of eigen values to ask for (proportioned of the total available)", 
		metaVar="INTEGER"
	)
	public double eigsel = 0.05;
	
	


	private Iterator<Double> eigIter;
	private Iterator<Double> mpIter;

	private int currentMP;
	
	@Option(
		name = "--spectral-clustering-mode",
		aliases = "-scm",
		required = true,
		usage = "The experiment mode used to cluster spectral clustering",
		handler = ProxyOptionHandler.class
	)
	SpatialClustererSetupOption experimentSetupMode = null;
	SpatialClustererSetupMode experimentSetupModeOp = null;

	
	double currentEig = -1;
	@Override
	public void setup() {
		this.experimentSetupModeOp.setup();
		if(this.eig.size() == 0){
			this.eigIter = new DoubleRange(eigStart, eigDelta, eigEnd).iterator();
		}
		else{
			this.eigIter = eig.iterator();
		}
	}
	@Override
	public boolean hasNextSetup() {
		return experimentSetupModeOp.hasNextSetup() || this.eigIter.hasNext();
	}
	
	@Override
	public NamedClusterer nextClusterer() {
		if(!experimentSetupModeOp.hasNextSetup() || this.currentEig == -1){
			experimentSetupModeOp.setup();
			this.currentEig = this.eigIter.next();
		}
		NamedSpecClusterConf nextClusterer = experimentSetupModeOp.nextClusterer();		
		NamedClusterer ret = new NamedClusterer();
		nextClusterer.conf.eigenChooser = new AbsoluteValueEigenChooser(currentEig, eigsel);
		ret.clusterer = new DoubleSpectralClustering(nextClusterer.conf);
		ret.name = String.format("eiggap=%2.2f/%s/%s",this.currentEig,this.experimentSetupMode.name(),nextClusterer.name);
		return ret;
	}
	

}
