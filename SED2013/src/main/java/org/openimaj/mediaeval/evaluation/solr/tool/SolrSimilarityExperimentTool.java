package org.openimaj.mediaeval.evaluation.solr.tool;

import java.io.File;
import java.io.PrintWriter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.openimaj.data.DoubleRange;
import org.openimaj.experiment.ExperimentContext;
import org.openimaj.experiment.ExperimentRunner;
import org.openimaj.experiment.evaluation.cluster.processor.Clusterer;
import org.openimaj.io.IOUtils;
import org.openimaj.mediaeval.evaluation.solr.DBSCANSolrSimilarityExperiment;
import org.openimaj.mediaeval.evaluation.solr.SolrSimilarityMatrixClustererExperiment;
import org.openimaj.tools.twitter.options.TwitterPreprocessingToolOptions;

import ch.akuhn.matrix.SparseMatrix;

/**
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class SolrSimilarityExperimentTool extends SolrSimilarityMatrixClustererExperiment {

	private static SolrSimilarityExperimentToolOptions options;

	public SolrSimilarityExperimentTool(String similarityFile,String indexFile, int start, int end) {
		super(similarityFile, indexFile, start, end);
	}

	@Override
	public Clusterer<SparseMatrix> prepareClusterer() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main(String[] args) {
		options = new SolrSimilarityExperimentToolOptions(args);
		
		// for each experimental configuration
			// run the experiment
			// save the output
		// end
		while(options.hasNextExperiment()){
			options.performNextExperiment();
		}
		
		
	}

}
