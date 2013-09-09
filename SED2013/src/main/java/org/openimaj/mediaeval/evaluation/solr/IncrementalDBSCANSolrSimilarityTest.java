package org.openimaj.mediaeval.evaluation.solr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;

import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.experiment.ExperimentContext;
import org.openimaj.experiment.ExperimentRunner;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.ml.clustering.IndexClusters;

/**
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class IncrementalDBSCANSolrSimilarityTest {
	public static void main(String[] args) {
		String similarityFile = "/Users/ss/Experiments/sed2013/training.sed2013.solr.matrixlib.allparts.sparsematrix.combined/ALL/aggregationMean.mat";
		String indexFile = "/Users/ss/Experiments/solr/sed2013_train_v2/data/index";
		
//		int start = 0;
//		for (int end = 1000; end < 5000; end+=200) {
//			DBSCANSolrSimilarityExperiment exp = new DBSCANSolrSimilarityExperiment(similarityFile, indexFile, start, end);
//			ExperimentContext c = ExperimentRunner.runExperiment(exp);
//			IndexClusters ic = new IndexClusters(exp.analysis.estimated);
//			System.out.println(ic);
//		}
		
		DBSCANSolrSimilarityExperiment exp = new DBSCANSolrSimilarityExperiment(similarityFile, indexFile, 0, -1);
		exp.setup();
		MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto> gt = exp.getGroundtruth();
		int maxClusterSize = -Integer.MAX_VALUE;
		int maxClusterRange = -Integer.MAX_VALUE;
		System.out.println("Checking cluster distributions");
		ArrayList<Integer> ranges = new ArrayList<Integer>();
		for (Entry<Integer, ListDataset<IndexedPhoto>> indexedPhoto : gt.entrySet()) {
			ListDataset<IndexedPhoto> ds = indexedPhoto.getValue();
			maxClusterSize = Math.max(ds.size(), maxClusterSize);
			long min = Integer.MAX_VALUE;
			long max = -Integer.MAX_VALUE;
			for (IndexedPhoto ip : ds) {
				min = Math.min(ip.first, min);
				max = Math.max(ip.first, max);
			}
			if(min == max){
				System.out.println("Zero length Cluster detected!");
				System.out.println(ds);
				System.out.println(ds.get(0).second.getId());
			}
			if(max - min > 10000){
				System.out.println("Huge Range Detected");
				System.out.println("Size: " + ds.size());
				System.out.println(ds.get(0).second.getId());
			}
			maxClusterRange = (int) Math.max(maxClusterRange, max - min);
			ranges.add((int) (max-min));
		}
		Collections.sort(ranges);
		System.out.println(ranges);
		System.out.println("Max cluster size: " + maxClusterSize);
		System.out.println("Max cluster range: " + maxClusterRange);
	}
}
