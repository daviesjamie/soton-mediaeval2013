package org.openimaj.mediaeval.evaluation.cluster.processor;

import java.util.List;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <T> the type of data to be clustered
 */
public interface Clusterer<T> {
	/**
	 * @param data
	 * @return Given a list of data items, cluster them by index
	 */
	public int[][] cluster(List<T> data);
}
