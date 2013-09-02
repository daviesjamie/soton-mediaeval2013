package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.util.filter.FilterUtils;
import org.openimaj.util.function.Predicate;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultList;

/**
 * Basic diversifier that actually does nothing at all
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class FilteringDiversifier implements Diversifier {
	Predicate<ResultItem> predicate;

	public FilteringDiversifier(Predicate<ResultItem> predicate) {
		this.predicate = predicate;
	}

	public FilteringDiversifier(final List<Predicate<ResultItem>> predicates) {
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

	public FilteringDiversifier(final Predicate<ResultItem>... predicates) {
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

	@Override
	public final List<ObjectDoublePair<ResultItem>> diversify(ResultList original) {
		final ArrayList<ResultItem> filtered = FilterUtils.filter(original, predicate);

		return diversifyFiltered(filtered, original);
	}

	/**
	 * diversify the filtered results. Subclasses should override to do
	 * something sensible. This implementation just returns the filtered results
	 * with scores computed from the inverse of the rank.
	 * 
	 * @param filtered
	 * @param original
	 * @return
	 */
	protected List<ObjectDoublePair<ResultItem>> diversifyFiltered(ArrayList<ResultItem> filtered, ResultList original) {
		final List<ObjectDoublePair<ResultItem>> results = new ArrayList<ObjectDoublePair<ResultItem>>();

		int i = 0;
		for (final ResultItem ri : filtered) {
			results.add(ObjectDoublePair.pair(ri, 1.0 / (++i)));
		}

		return results;
	}
}
