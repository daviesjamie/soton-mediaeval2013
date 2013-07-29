package org.openimaj.stream.functions;

import org.openimaj.util.function.Predicate;

public class Not<T> implements Predicate<T> {

	private Predicate<T> pred;

	public Not(Predicate<T> pred) {
		this.pred = pred;
	}

	@Override
	public boolean test(T object) {
		return !this.pred.test(object);
	}

}
