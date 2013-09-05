package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates;

import org.openimaj.util.function.Predicate;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;

/**
 * Filter based on the number of views
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 */
public class NumViews implements Predicate<ResultItem> {
	int minViews;

	public NumViews(int minViews) {
		this.minViews = minViews;
	}

	@Override
	public boolean test(ResultItem object) {
		if (object.views > minViews)
			return true;
		return false;
	}
}
