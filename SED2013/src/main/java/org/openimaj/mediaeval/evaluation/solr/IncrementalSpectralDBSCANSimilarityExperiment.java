package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.ParseException;
import org.openimaj.experiment.ExperimentContext;
import org.openimaj.experiment.ExperimentRunner;
import org.openimaj.experiment.evaluation.cluster.processor.Clusterer;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.ml.clustering.SpatialClusterer;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;
import org.openimaj.ml.clustering.dbscan.DoubleNNDBSCAN;
import org.openimaj.ml.clustering.incremental.IncrementalSparseClusterer;
import org.openimaj.ml.clustering.spectral.AbsoluteValueEigenChooser;
import org.openimaj.ml.clustering.spectral.DoubleSpectralClustering;
import org.openimaj.ml.clustering.spectral.GraphLaplacian;
import org.openimaj.ml.clustering.spectral.SpectralClusteringConf;
import org.openimaj.ml.clustering.spectral.SpectralIndexedClusters;
import org.openimaj.vis.general.BarVisualisationBasic;

import ch.akuhn.matrix.SparseMatrix;

/**
 * A {@link SolrSimilarityMatrixClustererExperiment} which launches the experiment using DBSCAN
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class IncrementalSpectralDBSCANSimilarityExperiment extends SolrSimilarityMatrixClustererExperiment{
	private int windowSize;
	private double threshold;

	/**
	 * @param similarityFile
	 * @param indexFile
	 * @param start
	 * @param end
	 * @param windowSize 
	 */
	public IncrementalSpectralDBSCANSimilarityExperiment(String similarityFile, String indexFile, int start, int end, int windowSize, double threshold) {
		super(similarityFile, indexFile, start, end);
		this.windowSize = windowSize;
		this.threshold = threshold;
	}

	@Override
	public Clusterer<SparseMatrix> prepareClusterer() {
		DoubleNNDBSCAN inner = new DoubleNNDBSCAN(
			0.5, 2, new DoubleNearestNeighboursExact.Factory(DoubleFVComparison.EUCLIDEAN)
		);
		inner.setNoiseAsClusters(true);
		SpectralClusteringConf<double[]> conf = new SpectralClusteringConf<double[]>(inner, new GraphLaplacian.Normalised());
//		conf.eigenChooser = new AutoSelectingEigenChooser(50, 0.05);
//		conf.eigenChooser = new HardCodedEigenChooser((int) (this.similarityMatrix.matrix().rowCount() * 0.20));
		conf.eigenChooser = new AbsoluteValueEigenChooser(0.5, 0.01);
		return new IncrementalSparseClusterer(new DoubleSpectralClustering(conf),windowSize,threshold);
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws IOException, XMLStreamException, ParseException {
		Appender app = (Appender) Logger.getRootLogger().getAllAppenders().nextElement();
		int start = Integer.parseInt(args[0]);
		int end = Integer.parseInt(args[1]);
		int windowSize = Integer.parseInt(args[2]);
		double threshold = Double.parseDouble(args[3]);
		String experimentOut = args[4] + "/incrementalspectral";
		File expOut = new File(experimentOut,String.format("%d_%d",start,end));
		expOut = new File(expOut,"windowsize="+windowSize+ ",threshold="+threshold);
		Logger.getRootLogger().addAppender(new FileAppender(app.getLayout(),new File(expOut,"log").getAbsolutePath()));
		if(!expOut.exists()){
			expOut.mkdirs();
		}
		String indexFile = args[5];
		
		for (int i = 6; i < args.length; i++) {
			String similarityMatrix = args[i];
			String[] split = similarityMatrix.split("/");
			String name = split[split.length-1];
			File dataSourceOut = new File(expOut,name);
			PrintWriter reportWriter = new PrintWriter(new File(dataSourceOut,"report.txt"));
			PrintWriter correctWriter = new PrintWriter(new File(dataSourceOut,"correct.txt"));
			PrintWriter estimatedWriter = new PrintWriter(new File(dataSourceOut,"estimated.txt"));
			IncrementalSpectralDBSCANSimilarityExperiment exp = new IncrementalSpectralDBSCANSimilarityExperiment(similarityMatrix, indexFile, start, end, windowSize, threshold);
			ExperimentContext c = ExperimentRunner.runExperiment(exp);
			exp.writeIndexClusters(correctWriter,exp.analysis.correct);
			exp.writeIndexClusters(estimatedWriter,exp.analysis.estimated);
			reportWriter.println(c);
			System.out.println(c);
			reportWriter.flush();
			reportWriter.close();
		}
		
	}

}
