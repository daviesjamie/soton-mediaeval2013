package org.openimaj.mediaeval.evaluation.cluster.analyser;

import gnu.trove.map.hash.TIntIntHashMap;

import java.util.HashMap;
import java.util.Map;

import org.openimaj.util.pair.IntIntPair;

import twitter4j.internal.logging.Logger;


/**
 * A set of measures used to evaulate clustering. 
 * These metrics are taken from: http://nlp.stanford.edu/IR-book/html/htmledition/evaluation-of-clustering-1.html
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class MEClusterAnalyser implements ClusterAnalyser<MEAnalysis>{
	private Logger logger = Logger.getLogger(MEClusterAnalyser.class);

	@Override
	public MEAnalysis analyse(int[][] correct, int[][] estimated) {
		Map<Integer,Integer> invCor = invert(correct);
		Map<Integer,Integer> invEst = invert(estimated);
		MEAnalysis ret = new MEAnalysis();
		ret.purity = purity(correct,estimated,invCor,invEst);
		ret.nmi = nmi(correct,estimated,invCor,invEst);
		return ret;
	}
	
	private double nmi(int[][] c, int[][] e, Map<Integer, Integer> ic, Map<Integer, Integer> ie) {
		double N = Math.max(ic.size(), ie.size());
		double mi = mutualInformation(N, c,e,ic,ie);
		logger.debug(String.format("Iec = %2.5f",mi));
		double ent_e = entropy(e,N);
		logger.debug(String.format("He = %2.5f",ent_e));
		double ent_c = entropy(c,N);
		logger.debug(String.format("Hc = %2.5f",ent_c));
		return mi / ((ent_e + ent_c)/2);
	}
	
	/**
	 * Maximum liklihood estimate of the entropy
	 * @param clusters
	 * @param N
	 * @return
	 */
	private double entropy(int[][] clusters, double N) {
		double total = 0;
		for (int k = 0; k < clusters.length; k++) {
			logger .debug(String.format("%2.1f/%2.1f * log2 ((%2.1f / %2.1f) )",(double)clusters[k].length,(double)N,(double)clusters[k].length,(double)N));
			double prop = clusters[k].length / N;
			total += prop * log2(prop);
		}
		return -total;
	}

	private double log2(double prop) {
		if(prop == 0) return 0;
		return Math.log(prop)/Math.log(2);
	}

	/**
	 * Maximum Liklihood estimate of the mutual information
	 * 
	 * @param c
	 * @param e
	 * @param ic
	 * @param ie
	 * @return
	 */
	private double mutualInformation(double N, int[][] c, int[][] e, Map<Integer, Integer> ic, Map<Integer, Integer> ie) {
		double mi = 0;
		for (int k = 0; k < e.length; k++) {
			double n_e = e[k].length;
			for (int j = 0; j < c.length; j++) {
				double n_c = c[j].length;
				double both = 0;
				for (int i = 0; i < e[k].length; i++) {
					if(ic.get(e[k][i]) == j) both++;
				}
				double normProp = (both * N)/(n_c * n_e);
				logger.debug(String.format("normprop = %2.5f",normProp));
				double sum = (both / N) * (log2(normProp));
				mi += sum;
				
				logger.debug(String.format("%2.1f/%2.1f * log2 ((%2.1f * %2.1f) / (%2.1f * %2.1f)) = %2.5f",both,N,both,N,n_c,n_e,sum));
			}
		}
		return mi;
	}

	/**
	 * Calculate purity. The *class* of a given point is its cluster in the ground truth.
	 * 
	 * @param correct
	 * @param estimated
	 * @param invCor
	 * @param invEst
	 * @return
	 */
	private double purity(int[][] correct, int[][] estimated, Map<Integer, Integer> invCor, Map<Integer, Integer> invEst) {
		double sumPurity = 0;
		for (int k = 0; k < estimated.length; k++) {
			IntIntPair maxClassCount = findMaxClassCount(estimated[k],invCor);
			sumPurity+=maxClassCount.second;
		}
		return sumPurity/invEst.size();
	}

	private IntIntPair findMaxClassCount(int[] is, Map<Integer, Integer> invCor) {
		TIntIntHashMap classCounts = new TIntIntHashMap();
		int max = 0;
		int c = 0;
		for (int i : is) {
			Integer c_i = invCor.get(i);
			int count = classCounts.adjustOrPutValue(c_i, 1, 1);
			if(count > max){
				max = count;
				c = c_i;
			}
		}
		return IntIntPair.pair(c, max);
	}

	private Map<Integer, Integer> invert(int[][] clustered) {
		int cluster = 0;
		Map<Integer, Integer> ret = new HashMap<Integer, Integer>();
		for (int[] is : clustered) {
			for (int index : is) {
				ret.put(index, cluster);
			}
			cluster++;
		}
		return ret;
	}
}
