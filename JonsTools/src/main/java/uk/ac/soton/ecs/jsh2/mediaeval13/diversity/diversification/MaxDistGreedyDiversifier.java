package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparator;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;

/**
 * Diversifier based on greedily choosing the item with the max distance from
 * the previously chosen items as per our ImageCLEF09 paper.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 */
public class MaxDistGreedyDiversifier implements Diversifier {
	public static enum AggregationStrategy {
		Min {
			@Override
			public double aggregate(double prevDistance, double newDistance) {
				if (prevDistance < newDistance)
					return prevDistance;
				return newDistance;
			}
		},
		Product {
			@Override
			public double aggregate(double prevDistance, double newDistance) {
				return prevDistance * newDistance;
			}
		},
		Sum {
			@Override
			public double aggregate(double prevDistance, double newDistance) {
				return prevDistance + newDistance;
			}
		};
		public abstract double aggregate(double prevDistance, double newDistance);
	}

	private AggregationStrategy aggregationStrategy;
	private DoubleFVComparator comparator;
	private FeatureExtractor<DoubleFV, ResultItem> extractor;

	public MaxDistGreedyDiversifier(FeatureExtractor<DoubleFV, ResultItem> extr, DoubleFVComparator comp,
			AggregationStrategy aggr)
	{
		this.aggregationStrategy = aggr;
		this.comparator = comp;
		this.extractor = extr;
	}

	@Override
	public List<ObjectDoublePair<ResultItem>> diversify(List<ObjectDoublePair<ResultItem>> input) {
		final List<ResultItem> working = ObjectDoublePair.getFirst(input);
		final List<ObjectDoublePair<ResultItem>> results = new ArrayList<ObjectDoublePair<ResultItem>>();

		results.add(ObjectDoublePair.pair(working.remove(0), 1));

		int i = 1;
		while (results.size() < 50 && working.size() > 0) {
			final ResultItem rx = findBest(results, working);
			working.remove(rx);
			results.add(ObjectDoublePair.pair(rx, 1.0 / (++i)));
		}

		return results;
	}

	private ResultItem findBest(List<ObjectDoublePair<ResultItem>> results, List<ResultItem> working) {
		double bestDist = -Double.MAX_VALUE;
		ResultItem bestItem = null;

		for (final ResultItem ri : working) {
			final double dist = computeAggregateDistance(ri, results);

			if (dist > bestDist) {
				bestDist = dist;
				bestItem = ri;
			}
		}

		return bestItem;
	}

	private double computeAggregateDistance(ResultItem ri, List<ObjectDoublePair<ResultItem>> results) {
		final DoubleFV rif = extractor.extractFeature(ri);

		double prevDistance = comparator.compare(rif, extractor.extractFeature(results.get(0).first));

		for (int i = 1; i < results.size(); i++) {
			final double newDistance = comparator.compare(rif, extractor.extractFeature(results.get(i).first));

			prevDistance = aggregationStrategy.aggregate(prevDistance, newDistance);
		}

		return prevDistance;
	}
}
