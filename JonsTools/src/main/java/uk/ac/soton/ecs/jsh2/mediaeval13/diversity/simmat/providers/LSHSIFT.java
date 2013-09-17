package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.providers;

import gnu.trove.TIntLongHashMap;

import java.io.IOException;
import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.graph.BuildSIFTGraph;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.SimMatProvider;
import ch.akuhn.matrix.SparseMatrix;

public class LSHSIFT implements SimMatProvider {

	@Override
	public SparseMatrix computeSimilarityMatrix(List<ObjectDoublePair<ResultItem>> results) {
		try {
			final List<ResultItem> flatResults = ObjectDoublePair.getFirst(results);
			final SimpleWeightedGraph<Long, DefaultWeightedEdge> graph = BuildSIFTGraph.buildGraph(
					flatResults, 2, 20);

			final TIntLongHashMap mapping = new TIntLongHashMap();
			for (int i = 0; i < flatResults.size(); i++)
				mapping.put(i, flatResults.get(i).id);

			final SparseMatrix sm = new SparseMatrix(flatResults.size(), flatResults.size());
			for (int i = 0; i < flatResults.size(); i++) {
				for (int j = 0; j < flatResults.size(); j++) {
					final DefaultWeightedEdge e = graph.getEdge(mapping.get(i), mapping.get(j));
					if (e != null) {
						sm.put(i, j, 1);// graph.getEdgeWeight(e));
					}
					if (i == j) {
						sm.put(i, j, 1);
					}
				}
			}

			return sm;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
}
