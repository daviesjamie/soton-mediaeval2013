package org.openimaj.mediaeval.evaluation.solr;

import java.util.Arrays;

import org.openimaj.experiment.ExperimentContext;
import org.openimaj.experiment.ExperimentRunner;
import org.openimaj.ml.clustering.IndexClusters;


/**
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class IncrementalSpectralDBSCANSolrSimilarityTest {
	public static void main(String[] args) {
		String similarityFile = "/Users/ss/Experiments/sed2013/training.sed2013.solr.matrixlib.allparts.sparsematrix.combined/ALL/aggregationMean.mat";
		String indexFile = "/Users/ss/Experiments/solr/sed2013_train_v2/data/index";
		
		int start = 0;
		for (int end = 1000; end < 5000; end+=200) {
			SpectralDBSCANSimilarityExperiment exp = new SpectralDBSCANSimilarityExperiment(similarityFile, indexFile, start, end);
			ExperimentContext c = ExperimentRunner.runExperiment(exp);
			for (int[] string : exp.analysis.estimated) {
				Arrays.sort(string);
			}
			IndexClusters ic = new IndexClusters(exp.analysis.estimated);
			System.out.println(ic);
		}
	}
}
