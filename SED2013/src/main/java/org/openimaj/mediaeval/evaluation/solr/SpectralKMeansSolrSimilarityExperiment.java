package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.stream.XMLStreamException;

import org.apache.lucene.queryparser.classic.ParseException;
import org.openimaj.experiment.ExperimentContext;
import org.openimaj.experiment.ExperimentRunner;
import org.openimaj.experiment.evaluation.cluster.processor.Clusterer;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.knn.DoubleNearestNeighbours;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.knn.approximate.DoubleNearestNeighboursKDTree;
import org.openimaj.ml.clustering.DoubleCentroidsResult;
import org.openimaj.ml.clustering.SpatialClusterer;
import org.openimaj.ml.clustering.SpatialClusters;
import org.openimaj.ml.clustering.kmeans.DoubleKMeans;
import org.openimaj.ml.clustering.kmeans.KMeansConfiguration;
import org.openimaj.ml.clustering.spectral.AbsoluteValueEigenChooser;
import org.openimaj.ml.clustering.spectral.ChangeDetectingEigenChooser;
import org.openimaj.ml.clustering.spectral.DoubleSpectralClustering;
import org.openimaj.ml.clustering.spectral.GraphLaplacian;
import org.openimaj.ml.clustering.spectral.HardCodedEigenChooser;
import org.openimaj.ml.clustering.spectral.SpectralClusteringConf;
import org.openimaj.ml.clustering.spectral.SpectralIndexedClusters;
import org.openimaj.ml.clustering.spectral.SpectralClusteringConf.ClustererProvider;
import org.openimaj.util.function.Function;
import org.openimaj.util.pair.IndependentPair;
import org.openimaj.vis.general.BarVisualisationBasic;

import ch.akuhn.matrix.SparseMatrix;

/**
 * A {@link SolrSimilarityMatrixClustererExperiment} which launches the experiment using DBSCAN
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class SpectralKMeansSolrSimilarityExperiment extends SolrSimilarityMatrixClustererExperiment{
	/**
	 * @param similarityFile
	 * @param indexFile
	 * @param start
	 * @param end
	 */
	public SpectralKMeansSolrSimilarityExperiment(String similarityFile, String indexFile, int start, int end) {
		super(similarityFile, indexFile, start, end);
	}

	@Override
	public Clusterer<SparseMatrix> prepareClusterer() {
		
		ClustererProvider<double[]> func = new ClustererProvider<double[]>() {
			@Override
			public SpatialClusterer<? extends SpatialClusters<double[]>, double[]> apply(IndependentPair<double[], double[][]> in) {
				DoubleKMeans inner = DoubleKMeans.createExact(69, 1000);
				inner.getConfiguration().setNearestNeighbourFactory(new DoubleNearestNeighboursExact.Factory(DoubleFVComparison.COSINE_SIM));
				return inner;
			}
			
			@Override
			public String toString() {
				return DoubleKMeans.createExact(69, 1000).toString();
			}
		};
		SpectralClusteringConf<double[]> conf = new SpectralClusteringConf<double[]>(func);
		conf.laplacian = new GraphLaplacian.Normalised();
//		conf.eigenChooser = new AutoSelectingEigenChooser(50, 0.05);
//		conf.eigenChooser = new HardCodedEigenChooser((int) (this.similarityMatrix.matrix().rowCount() * 0.20));
		conf.eigenChooser = new AbsoluteValueEigenChooser(0.15, 0.05);
		return new DoubleSpectralClustering(conf)
		{
			@Override
			public SpectralIndexedClusters cluster(SparseMatrix data) {
				SpectralIndexedClusters ret = super.cluster(data);
				BarVisualisationBasic vis = new BarVisualisationBasic(800, 200);
				vis.setData(ret.eigenValues());
				vis.showWindow("Cluster Eigen Values");
				return ret;
			}
		}
		;
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws IOException, XMLStreamException, ParseException {
		int start = Integer.parseInt(args[0]);
		int end = Integer.parseInt(args[1]);
		String experimentOut = args[2] + "/spectral_kmeans";
		File expOut = new File(experimentOut,String.format("%d_%d",start,end));
		if(!expOut.exists()){
			expOut.mkdirs();
		}
		String indexFile = args[3];
		PrintWriter reportWriter = new PrintWriter(new File(expOut,"report.txt"));
		PrintWriter correctWriter = new PrintWriter(new File(expOut,"correct.txt"));
		PrintWriter estimatedWriter = new PrintWriter(new File(expOut,"estimated.txt"));
		
		for (int i = 4; i < args.length; i++) {
			String similarityMatrix = args[i];
			String[] split = similarityMatrix.split("/");
			String name = split[split.length-1];
			SolrSimilarityMatrixClustererExperiment exp = new SpectralKMeansSolrSimilarityExperiment(similarityMatrix, indexFile, start, end);
			ExperimentContext c = ExperimentRunner.runExperiment(exp);
			
			reportWriter.println(c);
			exp.writeIndexClusters(correctWriter,exp.analysis.correct);
			exp.writeIndexClusters(estimatedWriter,exp.analysis.estimated);
			reportWriter.flush();
		}
		reportWriter.close();
		
	}

}
