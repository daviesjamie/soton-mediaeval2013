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
import org.openimaj.io.IOUtils;
import org.openimaj.ml.clustering.dbscan.SimilarityDBSCAN;

/**
 * A {@link SolrSimilarityMatrixClustererExperiment} which launches the experiment using DBSCAN
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class DBSCANSolrSimilarityExperiment extends SolrSimilarityMatrixClustererExperiment{
	/**
	 * @param similarityFile
	 * @param indexFile
	 * @param start
	 * @param end
	 */
	public DBSCANSolrSimilarityExperiment(String similarityFile, String indexFile, int start, int end) {
		super(similarityFile, indexFile, start, end);
	}

	double eps;
	@Override
	public SimilarityDBSCAN prepareClusterer() {
		return new SimilarityDBSCAN(eps, 5);
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
		String experimentOut = args[2] + "/dbscan";
		File expOut = new File(experimentOut,String.format("%d_%d",start,end));
		if(!expOut.exists()){
			expOut.mkdirs();
		}
		String indexFile = args[3];
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		DoubleRange r = new DoubleRange(0.4,0.05,0.9);
		PrintWriter reportWriter = new PrintWriter(new File(expOut,"report.txt"));
		
		for (int i = 4; i < args.length; i++) {
			String similarityMatrix = args[i];
			String[] split = similarityMatrix.split("/");
			String name = split[split.length-1];
			DBSCANSolrSimilarityExperiment exp = new DBSCANSolrSimilarityExperiment(similarityMatrix, indexFile, start, end);
			for (Double d : r) {
				exp.eps = d;
				ExperimentContext c = ExperimentRunner.runExperiment(exp);
				dataset.addValue(exp.f1score.score(), name, String.format("%2.2f",d));
				reportWriter.println(c);
				reportWriter.flush();
			}
		}
		reportWriter.close();
		IOUtils.writeToFile(dataset, new File(expOut,"results.jchart"));
		dataset = IOUtils.readFromFile(new File(expOut,"results.jchart"));
		JFreeChart line = ChartFactory.createLineChart("DBSCAN Clustering", "DBSCAN eps", "f1score", dataset , PlotOrientation.VERTICAL, true, false, false);
		ChartUtilities.saveChartAsPNG(new File(expOut,"results.png"), line, 800, 600);
		
	}

}
