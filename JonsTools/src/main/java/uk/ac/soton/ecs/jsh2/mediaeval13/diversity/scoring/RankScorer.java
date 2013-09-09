package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.scoring;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultList;

/**
 * Basic scorer that just computes scores based on the rank
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class RankScorer implements Scorer {
	@Override
	public List<ObjectDoublePair<ResultItem>> score(ResultList input) {
		final List<ObjectDoublePair<ResultItem>> results = new ArrayList<ObjectDoublePair<ResultItem>>();

		int i = 0;
		for (final ResultItem ri : input) {
			results.add(ObjectDoublePair.pair(ri, 1.0 / (++i)));
		}

		return results;
	}
}
