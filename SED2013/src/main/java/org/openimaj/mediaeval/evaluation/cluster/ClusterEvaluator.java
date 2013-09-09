package org.openimaj.mediaeval.evaluation.cluster;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.openimaj.experiment.evaluation.AnalysisResult;
import org.openimaj.experiment.evaluation.cluster.analyser.FScoreClusterAnalyser;
import org.openimaj.experiment.evaluation.cluster.analyser.RandomBaselineSMEAnalysis;
import org.openimaj.experiment.evaluation.cluster.analyser.RandomBaselineSMEClusterAnalyser;
import org.openimaj.ml.clustering.IndexClusters;
import org.openimaj.util.pair.IntDoublePair;
import org.openimaj.util.queue.BoundedPriorityQueue;
import org.openimaj.video.ArrayBackedVideo;

/**
 * Load a ground truth and compare to a set of clusters
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class ClusterEvaluator {
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--groundtruth-clusters", 
		aliases="-c", 
		required=true, 
		usage="The ground truth file", 
		metaVar="STRING"
	)
	public String groundTruth;
	
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--estimated-clusters", 
		aliases="-e", 
		required=true, 
		usage="The estimates clusters file", 
		metaVar="STRING"
	)
	public String estimatedClusters;

	private String[] args;

	private RandomBaselineSMEClusterAnalyser analyser;
	
	public ClusterEvaluator(String[] args) {
		this.args = args;
		analyser = new RandomBaselineSMEClusterAnalyser();
		this.prepare();
	}

	static int[][] loadClusters(String clusterfile) throws IOException{
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(clusterfile)));
		String line = null;
		Map<Integer,List<Integer>> clusters=  new HashMap<Integer, List<Integer>>();
		while((line = reader.readLine())!= null){
			String[] itemcluster = line.split(" ");
			int item = Integer.parseInt(itemcluster[0]);
			int cluster = Integer.parseInt(itemcluster[1]);
			List<Integer> clusteritems = clusters.get(cluster);
			if(clusteritems==null){
				clusters.put(cluster, clusteritems = new ArrayList<Integer>());
			}
			clusteritems.add(item);
		}
		int[][] clusterArr = new int[clusters.size()][];
		for (Entry<Integer, List<Integer>> is : clusters.entrySet()) {
			int clusterIndex = is.getKey();
			List<Integer> clusterList = is.getValue();
			clusterArr[clusterIndex] = new int[clusterList.size()];
			for (int i = 0; i < clusterArr[clusterIndex].length; i++) {
				clusterArr[clusterIndex][i] = clusterList.get(i);
			}
		}
		reader.close();
		return clusterArr;
	}
	
	private void prepare() {
		final CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
			setup();
		} catch (final CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: java ... [options...] ");
			parser.printUsage(System.err);
			System.exit(1);
		}
	}

	private void setup() {
	}
	
	public static void main(String[] args) throws IOException {
		ClusterEvaluator eval = new ClusterEvaluator(args);
		int[][] gt = loadClusters(eval.groundTruth);
		int[][] est = loadClusters(eval.estimatedClusters);
		Map<String, AnalysisResult> res = eval.evaluate(gt,est);
		
		for (Entry<String, AnalysisResult> is : res.entrySet()) {
			System.out.printf("%s: %s\n",is.getKey(),is.getValue());
		}
		
		Map<Integer, IntDoublePair> stability = calculateStability(gt, est);
		BoundedPriorityQueue<Map.Entry<Integer, IntDoublePair>> mostStable = new BoundedPriorityQueue<Map.Entry<Integer,IntDoublePair>>(10,new MostStableComparator());
		BoundedPriorityQueue<Map.Entry<Integer, IntDoublePair>> leastStable = new BoundedPriorityQueue<Map.Entry<Integer,IntDoublePair>>(10,new LeastStableComparator());
		
		for (Entry<Integer, IntDoublePair> entry : stability.entrySet()) {
			mostStable.offer(entry);
			leastStable.offer(entry);
		}
		
		System.out.println("Most stable:");
		printStable(mostStable,gt,est);
		System.out.println("Least stable:");
		printStable(leastStable,gt,est);
	}
	
	private static void printStable(BoundedPriorityQueue<Entry<Integer, IntDoublePair>> stabqueue,int[][] c, int[][] e) {
		while(!stabqueue.isEmpty()){
			Entry<Integer, IntDoublePair> item = stabqueue.pollTail();
			int cindex = item.getKey();
			int eindex = item.getValue().first;
			String cstring = Arrays.toString(c[cindex]);
			String estring = eindex>0 ? Arrays.toString(e[eindex]) : "None";
			System.out.printf(
					"GroundTruth cluster %d and EstimatedCluster %d, stability: %2.5f\n%s\n%s\n",
					cindex,
					eindex,
					item.getValue().second,
					cstring,
					estring
			);
		}
		
	}

	static Map<Integer, IntDoublePair> calculateStability(int[][] clusters1, int[][] clusters2) {
		
		Map<Integer, IntDoublePair> stability = new HashMap<Integer, IntDoublePair>();
		for (int i = 0; i < clusters1.length; i++) {
			if(clusters1[i].length == 0) continue;
			double maxnmi = 0;
			int maxj = -1;
			int[][] correct = new int[][]{clusters1[i]};
			for (int j = 0; j < clusters2.length; j++) {
				int[][] estimated = new int[][]{clusters2[j]};
				double score = 0;
//				if(correct[0].length == 1 && estimated[0].length == 1){
//					// BOTH 1, either they are the same or not!
//					score = correct[0][0] == estimated[0][0] ? 1 : 0;
//				}
//				else{					
				score = new FScoreClusterAnalyser().analyse(correct, estimated).score();
//				}
				if(!Double.isNaN(score))
				{
					if(score > maxnmi){
						maxnmi = score;
						maxj = j;
					}
				}
			}
			stability.put(i, IntDoublePair.pair(maxj, maxnmi));
		}
		return stability;
	}

	private Map<String, AnalysisResult> evaluate(int[][] gt, int[][] est) {
		RandomBaselineSMEAnalysis analysis = analyser.analyse(gt, est);
		Map<String,AnalysisResult> toshow = new HashMap<String, AnalysisResult>();
		
		toshow.put("f1score", analysis.fscore);
		toshow.put("purity", analysis.purity);
		toshow.put("randIndex", analysis.randIndex);
		toshow.put("stats", analysis.stats);
		toshow.put("decision", analysis.fscore.getUnmodified().getDecisionAnalysis());
		
		return toshow;
	}
}
