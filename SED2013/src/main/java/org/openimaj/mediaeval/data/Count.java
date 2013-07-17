package org.openimaj.mediaeval.data;

import org.openimaj.util.function.Operation;


/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <T>
 */
public class Count<T> implements Operation<T>{

	public long seen;

	/**
	 */
	public Count() {
		this.seen = 0;
	}


	@Override
	public void perform(T object) {
		seen++;
	};

}
