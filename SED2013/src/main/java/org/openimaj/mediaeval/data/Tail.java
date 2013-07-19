package org.openimaj.mediaeval.data;

import org.openimaj.util.function.Predicate;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <T>
 */
public class Tail<T> implements Predicate<T> {

	private int n;
	private int seen;
	private int total;

	/**
	 * @param n
	 * @param total
	 */
	public Tail(int n, int total) {
		this.total = total;
		this.seen = 0;
		this.n = n;
	}

	@Override
	public boolean test(T object) {
		return seen++ + n >= this.total;
	}

}
