package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.scoring;

import java.util.List;

import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultList;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates.PreFilter;

public class FilteredScorer implements Scorer {
	Scorer scorer;
	PreFilter filter;

	public FilteredScorer(PreFilter filter, Scorer scorer) {
		this.filter = filter;
		this.scorer = scorer;
	}

	public FilteredScorer(PreFilter filter) {
		this.filter = filter;
		this.scorer = new RankScorer();
	}

	@Override
	public List<ObjectDoublePair<ResultItem>> score(ResultList input) {
		return scorer.score(filter.filter(input));
	}

}
