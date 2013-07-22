package org.openimaj.mediaeval.evaluation.datasets;

import org.openimaj.util.function.Predicate;
import org.openimaj.util.stream.Stream;

/**
 * {@link Stream} filter which skips a certain number of items
 * before allowing all items through
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 * @param <T>
 */
public class SkipFilter<T> implements Predicate<T> {

	private int skip;

	/**
	 * @param skip items to skip
	 */
	public SkipFilter(int skip) {
		this.skip = skip;
	}

	@Override
	public boolean test(T object) {
		if(skip == 0) return true;
		else{
			skip --;
			return false;
		}
	}

}
