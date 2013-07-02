package org.openimaj.mediaeval.evaluation.cluster;

import java.util.List;

import org.openimaj.experiment.evaluation.AnalysisResult;
import org.openimaj.experiment.evaluation.Evaluator;
import org.openimaj.mediaeval.evaluation.cluster.analyser.ClusterAnalyser;
import org.openimaj.mediaeval.evaluation.cluster.processor.DatasetClusterer;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <D> The type of data which the internal clusterer can cluster lists of
 * @param <T> The type of results the 
 */
public class ClusterEvaluator<D, T extends AnalysisResult> implements Evaluator<int[][],T>{
	
	private DatasetClusterer<D> gen;
	private List<D> data;
	private int[][] correct;
	private ClusterAnalyser<T> analyser;

	/**
	 * @param gen
	 * @param data
	 * @param clusters
	 * @param analyser 
	 */
	public ClusterEvaluator(DatasetClusterer<D> gen, List<D> data, int[][] clusters, ClusterAnalyser<T> analyser) {
		this.gen = gen;
		this.data = data;
		this.correct = clusters;
		this.analyser = analyser;
	}
	
	@Override
	public int[][] evaluate() {
		return this.gen.cluster(this.data);
	}

	@Override
	public T analyse(int[][] estimated) {
		return this.analyser.analyse(correct, estimated);
	}

}
