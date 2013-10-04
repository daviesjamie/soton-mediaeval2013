package org.openimaj.mediaeval.sandbox;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openimaj.mediaeval.evaluation.datasets.SED2013ExpOne;

public class ClusterStats {
	public static void main(String[] args) throws NumberFormatException, IOException {
		File csv = new File("/Users/ss/Experiments/sed2013/sed2013_dataset_train_gs.csv");
		Map<Integer, List<Long>> clusters = SED2013ExpOne.clusters(csv);
		double[] mmmt = minmaxmeantotal(clusters);
		System.out.printf("min(%2.2f), max(%2.2f), mean(%2.2f) , total(%2.2f) \n",mmmt[0],mmmt[1],mmmt[2],mmmt[3]);
		System.out.println("N clusters: " + clusters.size());
	}

	private static double[] minmaxmeantotal(Map<Integer, List<Long>> clusters) {
		double[] minmaxmean = new double[4];
		minmaxmean[0] = Double.MAX_VALUE;
		minmaxmean[1] = -Double.MAX_VALUE;
		for (Entry<Integer, List<Long>> d : clusters.entrySet()) {
			int count = d.getValue().size();
			minmaxmean[0] = Math.min(minmaxmean[0],count);
			minmaxmean[1] = Math.max(minmaxmean[1],count);
			minmaxmean[2] += (double)count/clusters.size();
			minmaxmean[3] += count;
			
		}
		return minmaxmean;
	}
}
