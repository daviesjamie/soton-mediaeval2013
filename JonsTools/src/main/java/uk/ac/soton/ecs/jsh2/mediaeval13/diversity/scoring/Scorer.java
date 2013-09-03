package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.scoring;

import java.util.List;

import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultList;

public interface Scorer {
	public List<ObjectDoublePair<ResultItem>> score(ResultList input);
}
