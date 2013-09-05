package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification;

import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.graph.BuildSIFTGraph;

public class NearDuplicatesFilter implements Diversifier {
	Diversifier div;

	public NearDuplicatesFilter() {

	}

	public NearDuplicatesFilter(Diversifier div) {
		this.div = div;
	}

	@Override
	public List<ObjectDoublePair<ResultItem>> diversify(List<ObjectDoublePair<ResultItem>> input) {
		try {
			final SimpleWeightedGraph<Long, DefaultWeightedEdge> graph = BuildSIFTGraph.buildGraph(
					ObjectDoublePair.getFirst(input), 3, 200);

			final ConnectivityInspector<Long, DefaultWeightedEdge> conn =
					new ConnectivityInspector<Long, DefaultWeightedEdge>(graph);

			final TLongIntHashMap idMap = new TLongIntHashMap();
			for (int i = 0; i < input.size(); i++)
				idMap.put(input.get(i).first.id, i);

			final TIntHashSet toRem = new TIntHashSet();
			for (final Set<Long> subgraph : conn.connectedSets()) {
				int bestId = -1;
				double bestScore = -Double.MAX_VALUE;

				for (final long fid : subgraph) {
					final int id = idMap.get(fid);
					final double score = input.get(id).second;

					toRem.add(id);
					if (bestScore < score) {
						bestScore = score;
						bestId = id;
					}
				}

				toRem.remove(bestId);
			}

			final List<ObjectDoublePair<ResultItem>> output = new ArrayList<ObjectDoublePair<ResultItem>>();
			for (int i = 0; i < input.size(); i++) {
				if (!toRem.contains(i)) {
					output.add(input.get(i));
				}
			}

			if (div != null)
				return div.diversify(output);
			else
				return output;
		} catch (final Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
