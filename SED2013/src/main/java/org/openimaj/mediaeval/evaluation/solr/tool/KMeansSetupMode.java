package org.openimaj.mediaeval.evaluation.solr.tool;

import org.kohsuke.args4j.Option;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.ml.clustering.SpatialClusterer;
import org.openimaj.ml.clustering.SpatialClusters;
import org.openimaj.ml.clustering.kmeans.DoubleKMeans;
import org.openimaj.ml.clustering.spectral.SpectralClusteringConf;
import org.openimaj.ml.clustering.spectral.SpectralClusteringConf.ClustererProvider;
import org.openimaj.util.pair.IndependentPair;

/**
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class KMeansSetupMode extends SpatialClustererSetupMode {
	
	
	@Option(
		name = "--distance-metric",
		aliases = "-dist",
		required = false,
		usage = "The distance metric used by the DBSCAP"
	)
	DoubleFVComparison comp = DoubleFVComparison.EUCLIDEAN;
	private boolean done;
	
	@Option(
		name = "--k-eig-prop",
		aliases = "-kprop",
		required = false,
		usage = "The proprotion of the number of selected eigen vectors to treat as the k of kmeans"
	)
	double kprop = 1.;
	
	@Option(
		name = "--kmeans-iter",
		aliases = "-iter",
		required = false,
		usage = "The number of iterations to perform kmeans"
	)
	int iter = 1000;

	
	@Override
	public void setup() {
		this.done = false;
	}
	
	@Override
	public boolean hasNextSetup() {
		return !done;
	}
	
	ClustererProvider<double[]> func = new ClustererProvider<double[]>() {
		@Override
		public SpatialClusterer<? extends SpatialClusters<double[]>, double[]> apply(IndependentPair<double[], double[][]> in) {
			DoubleKMeans inner = DoubleKMeans.createExact((int) (in.firstObject().length * kprop), iter);
			inner.getConfiguration().setNearestNeighbourFactory(new DoubleNearestNeighboursExact.Factory(comp));
			return inner;
		}
		
		@Override
		public String toString() {
			return DoubleKMeans.createExact(69, 1000).toString();
		}
	};
	
	@Override
	public NamedSpecClusterConf nextClusterer() {
		NamedSpecClusterConf nc = new NamedSpecClusterConf();
		nc.conf = new SpectralClusteringConf<double[]>(func);
		nc.name = String.format("iter=%d/kprop=%2.2f/dist=%s",this.iter,this.kprop,comp.name());
		this.done = true;
		return nc;
	}

}
