package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.stream.XMLStreamException;

import org.apache.lucene.queryparser.classic.ParseException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import /**
 * @param args
 * @throws IOException
 * @throws XMLStreamException
 * @throws ParseException
 */
org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.openimaj.data.DoubleRange;
import org.openimaj.experiment.ExperimentContext;
import org.openimaj.experiment.ExperimentRunner;
import org.openimaj.experiment.evaluation.cluster.processor.Clusterer;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.io.IOUtils;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.ml.clustering.SpatialClusterer;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;
import org.openimaj.ml.clustering.dbscan.DoubleNNDBSCAN;
import org.openimaj.ml.clustering.dbscan.SimilarityDBSCAN;
import org.openimaj.ml.clustering.spectral.DoubleSpectralClustering;
import org.openimaj.ml.clustering.spectral.GraphLaplacian;
import org.openimaj.ml.clustering.spectral.SpectralClusteringConf;

import ch.akuhn.matrix.SparseMatrix;

/**
 * A {@link SolrSimilarityMatrixClustererExperiment} which launches the experiment using DBSCAN
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class SpectralSolrSimilarityExperiment extends SolrSimilarityMatrixClustererExperiment{
	/**
	 * @param similarityFile
	 * @param indexFile
	 * @param start
	 * @param end
	 */
	public SpectralSolrSimilarityExperiment(String similarityFile, String indexFile, int start, int end) {
		super(similarityFile, indexFile, start, end);
	}

	@Override
	public Clusterer<SparseMatrix> prepareClusterer() {
		SpatialClusterer<DoubleDBSCANClusters,double[]> inner = new DoubleNNDBSCAN(
			0.2, 2, new DoubleNearestNeighboursExact.Factory(DoubleFVComparison.EUCLIDEAN)
		);
		SpectralClusteringConf<double[]> conf = new SpectralClusteringConf<double[]>(inner, new GraphLaplacian.Normalised());
		return new DoubleSpectralClustering(conf);
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
		String experimentOut = args[2] + "/spectral";
		File expOut = new File(experimentOut,String.format("%d_%d",start,end));
		if(!expOut.exists()){
			expOut.mkdirs();
		}
		String indexFile = args[3];
		PrintWriter reportWriter = new PrintWriter(new File(expOut,"report.txt"));
		
		for (int i = 4; i < args.length; i++) {
			String similarityMatrix = args[i];
			String[] split = similarityMatrix.split("/");
			String name = split[split.length-1];
			SpectralSolrSimilarityExperiment exp = new SpectralSolrSimilarityExperiment(similarityMatrix, indexFile, start, end);
			ExperimentContext c = ExperimentRunner.runExperiment(exp);
			reportWriter.println(c);
			reportWriter.flush();
		}
		reportWriter.close();
		
	}

}
