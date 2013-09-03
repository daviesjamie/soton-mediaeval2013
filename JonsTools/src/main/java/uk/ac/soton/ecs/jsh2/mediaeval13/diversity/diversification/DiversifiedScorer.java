package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification;

import java.util.List;

import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultList;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.scoring.RankScorer;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.scoring.Scorer;

public class DiversifiedScorer implements Scorer {
	Scorer scorer;
	Diversifier diversifier;

	public DiversifiedScorer(Scorer scorer, Diversifier diversifier) {
		this.scorer = scorer;
		this.diversifier = diversifier;
	}

	public DiversifiedScorer(Diversifier diversifier) {
		this.scorer = new RankScorer();
		this.diversifier = diversifier;
	}

	@Override
	public List<ObjectDoublePair<ResultItem>> score(ResultList input) {
		return diversifier.diversify(scorer.score(input));
	}
}
