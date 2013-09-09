package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification;

import gnu.trove.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.SimMatProvider;
import ch.akuhn.matrix.SparseMatrix;

/**
 * Diversifier based on greedily choosing the item with the max Similarity from
 * the previously chosen items as per our ImageCLEF09 paper.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 */
public class SimMatMaxDistGreedyDiversifier implements Diversifier {
	public static enum AggregationStrategy {
		Min {
			@Override
			public double aggregate(double prevSimilarity, double newSimilarity) {
				if (prevSimilarity < newSimilarity)
					return prevSimilarity;
				return newSimilarity;
			}
		},
		Max {
			@Override
			public double aggregate(double prevSimilarity, double newSimilarity) {
				if (prevSimilarity > newSimilarity)
					return prevSimilarity;
				return newSimilarity;
			}
		},
		Product {
			@Override
			public double aggregate(double prevSimilarity, double newSimilarity) {
				return prevSimilarity * newSimilarity;
			}
		},
		Sum {
			@Override
			public double aggregate(double prevSimilarity, double newSimilarity) {
				return prevSimilarity + newSimilarity;
			}
		};
		public abstract double aggregate(double prevSimilarity, double newSimilarity);
	}

	private AggregationStrategy aggregationStrategy;
	private SimMatProvider provider;

	public SimMatMaxDistGreedyDiversifier(SimMatProvider provider,
			AggregationStrategy aggr)
	{
		this.aggregationStrategy = aggr;
		this.provider = provider;
	}

	@Override
	public List<ObjectDoublePair<ResultItem>> diversify(List<ObjectDoublePair<ResultItem>> input) {
		final TIntArrayList workingIds = new TIntArrayList();
		final TIntArrayList resultIds = new TIntArrayList();
		for (int i = 0; i < input.size(); i++)
			workingIds.add(i);

		final SparseMatrix sm = provider.computeSimilarityMatrix(input);

		resultIds.add(workingIds.remove(0));

		final List<ObjectDoublePair<ResultItem>> results = new ArrayList<ObjectDoublePair<ResultItem>>();
		int i = 1;
		while (resultIds.size() < 50 && workingIds.size() > 0) {
			final int rx = findBest(resultIds, workingIds, sm);
			workingIds.remove(workingIds.indexOf(rx));
			resultIds.add(rx);
			results.add(ObjectDoublePair.pair(input.get(rx).first, 1.0 / (++i)));
		}

		return results;
	}

	private int findBest(TIntArrayList results, TIntArrayList working, SparseMatrix sm) {
		double bestSim = Double.MAX_VALUE;
		int bestItem = -1;

		for (final int ri : working.toNativeArray()) {
			final double sim = computeAggregateSimilarity(ri, results, sm);

			if (sim < bestSim) {
				bestSim = sim;
				bestItem = ri;
			}
		}

		return bestItem;
	}

	private double computeAggregateSimilarity(int from, TIntArrayList to, SparseMatrix sm) {
		double prevSim = sm.get(from, to.get(0));

		for (int i = 1; i < to.size(); i++) {
			final double newSim = sm.get(from, to.get(i));

			prevSim = aggregationStrategy.aggregate(prevSim, newSim);
		}

		return prevSim;
	}
}
