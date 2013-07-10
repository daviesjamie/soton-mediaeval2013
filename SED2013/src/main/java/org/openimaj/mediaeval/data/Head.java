package org.openimaj.mediaeval.data;

import org.openimaj.util.function.Predicate;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <T>
 */
public class Head<T> implements Predicate<T> {

	private int n;
	private int seen;

	/**
	 * @param n
	 */
	public Head(int n) {
		this.seen = 0;
		this.n = n;
	}

	@Override
	public boolean test(T object) {
		return seen++ < n;
	}

}
