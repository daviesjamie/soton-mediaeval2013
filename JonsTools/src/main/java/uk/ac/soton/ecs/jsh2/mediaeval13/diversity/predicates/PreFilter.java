package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.util.filter.FilterUtils;
import org.openimaj.util.function.Predicate;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultList;

/**
 * Apply predicates to a result list so that it only contains items for which
 * the predicates return true.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class PreFilter {
	Predicate<ResultItem> predicate;

	public PreFilter(final Predicate<ResultItem> predicate) {
		this.predicate = predicate;
	}

	public PreFilter(final List<Predicate<ResultItem>> predicates) {
		this.predicate = new Predicate<ResultItem>() {
			@Override
			public boolean test(ResultItem object) {
				for (final Predicate<ResultItem> p : predicates)
					if (!p.test(object))
						return false;
				return true;
			}
		};
	}

	public PreFilter(final Predicate<ResultItem>... predicates) {
		this.predicate = new Predicate<ResultItem>() {
			@Override
			public boolean test(ResultItem object) {
				for (final Predicate<ResultItem> p : predicates)
					if (!p.test(object))
						return false;
				return true;
			}
		};
	}

	public final ResultList filter(ResultList original) {
		final ArrayList<ResultItem> filtered = FilterUtils.filter(original, predicate);

		final ResultList rl = original.copy();
		rl.results.retainAll(filtered);
		return rl;
	}
}
