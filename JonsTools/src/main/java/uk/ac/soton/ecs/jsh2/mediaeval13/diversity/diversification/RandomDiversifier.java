package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;

/**
 * Basic diversifier that randomly shuffles the input
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class RandomDiversifier implements Diversifier {
	@Override
	public List<ObjectDoublePair<ResultItem>> diversify(List<ObjectDoublePair<ResultItem>> input) {
		final List<ResultItem> tmp = ObjectDoublePair.getFirst(input);
		Collections.shuffle(tmp);

		final List<ObjectDoublePair<ResultItem>> results = new ArrayList<ObjectDoublePair<ResultItem>>();

		int i = 0;
		for (final ResultItem ri : tmp) {
			results.add(ObjectDoublePair.pair(ri, 1.0 / (++i)));
		}

		return results;
	}
}
