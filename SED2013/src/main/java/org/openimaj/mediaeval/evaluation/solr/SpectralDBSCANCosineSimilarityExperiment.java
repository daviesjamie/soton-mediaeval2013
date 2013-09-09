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
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.ml.clustering.SpatialClusterer;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;
import org.openimaj.ml.clustering.dbscan.DoubleNNDBSCAN;
import org.openimaj.ml.clustering.spectral.AbsoluteValueEigenChooser;
import org.openimaj.ml.clustering.spectral.CachedDoubleSpectralClustering;
import org.openimaj.ml.clustering.spectral.ChangeDetectingEigenChooser;
import org.openimaj.ml.clustering.spectral.DoubleSpectralClustering;
import org.openimaj.ml.clustering.spectral.GraphLaplacian;
import org.openimaj.ml.clustering.spectral.HardCodedEigenChooser;
import org.openimaj.ml.clustering.spectral.SpectralClusteringConf;
import org.openimaj.ml.clustering.spectral.SpectralIndexedClusters;
import org.openimaj.vis.general.BarVisualisationBasic;

import ch.akuhn.matrix.SparseMatrix;

/**
 * A {@link SolrSimilarityMatrixClustererExperiment} which launches the experiment using DBSCAN
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class SpectralDBSCANCosineSimilarityExperiment extends SolrSimilarityMatrixClustererExperiment{
	private File eigenCache;

	/**
	 * @param eigencache
	 * @param similarityFile
	 * @param indexFile
	 * @param start
	 * @param end
	 */
	public SpectralDBSCANCosineSimilarityExperiment(File eigencache, String similarityFile, String indexFile, int start, int end) {
		super(similarityFile, indexFile, start, end);
		this.eigenCache = eigencache;
		this.eigenCache.mkdirs();
	}

	@Override
	public Clusterer<SparseMatrix> prepareClusterer() {
		SpatialClusterer<DoubleDBSCANClusters,double[]> inner = new DoubleNNDBSCAN(
			-0.8, 3, new DoubleNearestNeighboursExact.Factory(DoubleFVComparison.COSINE_DIST)
		);
		SpectralClusteringConf<double[]> conf = new SpectralClusteringConf<double[]>(inner, new GraphLaplacian.Normalised());
//		conf.eigenChooser = new AutoSelectingEigenChooser(50, 0.05);
//		conf.eigenChooser = new HardCodedEigenChooser(2);
		conf.eigenChooser = new AbsoluteValueEigenChooser(0.2, 0.01);
		return new CachedDoubleSpectralClustering(new File(eigenCache,conf.eigenChooser.toString()), conf);
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
		String experimentOut = args[2] + "/spectral_dbscancosine";
		File expOut = new File(experimentOut,String.format("%d_%d",start,end));
		if(!expOut.exists()){
			expOut.mkdirs();
		}
		String indexFile = args[3];
		PrintWriter reportWriter = new PrintWriter(new File(expOut,"report.txt"));
		PrintWriter correctWriter = new PrintWriter(new File(expOut,"correct.txt"));
		PrintWriter estimatedWriter = new PrintWriter(new File(expOut,"estimated.txt"));
		File eigencache = new File(expOut,"eigenvectors.eig");
		
		for (int i = 4; i < args.length; i++) {
			String similarityMatrix = args[i];
			String[] split = similarityMatrix.split("/");
			String name = split[split.length-1];
			SpectralDBSCANCosineSimilarityExperiment exp = new SpectralDBSCANCosineSimilarityExperiment(eigencache,similarityMatrix, indexFile, start, end);
			ExperimentContext c = ExperimentRunner.runExperiment(exp);
			exp.writeIndexClusters(correctWriter,exp.analysis.correct);
			exp.writeIndexClusters(estimatedWriter,exp.analysis.estimated);
			reportWriter.println(c);
			System.out.println(c);
			reportWriter.flush();
		}
		reportWriter.close();
		
	}

}
