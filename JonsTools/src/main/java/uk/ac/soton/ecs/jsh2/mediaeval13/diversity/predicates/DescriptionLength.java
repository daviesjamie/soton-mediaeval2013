package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates;

import org.openimaj.util.function.Predicate;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;

/**
 * Filter based on the max length of the description
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 */
public class DescriptionLength implements Predicate<ResultItem> {
	int maxLength;

	public DescriptionLength(int maxLength) {
		this.maxLength = maxLength;
	}

	@Override
	public boolean test(ResultItem object) {
		if (object.description.length() < maxLength)
			return true;
		return false;
	}
}
