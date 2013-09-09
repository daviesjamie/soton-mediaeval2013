package uk.ac.soton.ecs.jsh2.mediaeval13.placing.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.openimaj.image.MBFImage;

public class LSHSiftGraphSearcher implements VisualSearcher {
	private SimpleWeightedGraph<Long, DefaultWeightedEdge> graph;
	private boolean expand = false;
	private IndexSearcher meta;

	public LSHSiftGraphSearcher(File file, double minEdgeWeight, IndexSearcher meta)
			throws FileNotFoundException,
			IOException
	{
		this.graph = loadGraph(file, minEdgeWeight);
		this.meta = meta;
	}

	private static SimpleWeightedGraph<Long, DefaultWeightedEdge> loadGraph(File file, double minEdgeWeight)
			throws FileNotFoundException,
			IOException
	{
		final SimpleWeightedGraph<Long, DefaultWeightedEdge> graph = new SimpleWeightedGraph<Long, DefaultWeightedEdge>(
				DefaultWeightedEdge.class);

		final BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		while ((line = reader.readLine()) != null) {
			final String[] parts = line.split("\\s");

			final long from = Long.parseLong(parts[0]);
			final long to = Long.parseLong(parts[1]);
			final double weight = Double.parseDouble(parts[2]);

			if (weight < minEdgeWeight)
				continue;

			graph.addVertex(to);
			graph.addVertex(from);
			final DefaultWeightedEdge edge = graph.addEdge(from, to);
			if (edge != null)
				graph.setEdgeWeight(edge, weight);
		}
		reader.close();
		return graph;
	}

	public void setExpand(boolean b) {
		this.expand = b;
	}

	@Override
	public ScoreDoc[] search(MBFImage query, int numResults) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScoreDoc[] search(long flickrId, int numResults) throws IOException {
		List<ScoreDoc> results = new ArrayList<ScoreDoc>();

		if (!graph.containsVertex(flickrId))
			return new ScoreDoc[0];

		if (!expand) {
			final Set<DefaultWeightedEdge> edges = graph.edgesOf(flickrId);

			for (final DefaultWeightedEdge e : edges) {
				long r = graph.getEdgeSource(e);
				if (r == flickrId)
					r = graph.getEdgeTarget(e);

				final ScoreDoc sd = lookup(r);
				if (sd != null) {
					sd.score = (float) graph.getEdgeWeight(e);
					results.add(sd);
				}
			}
		} else {
			final ConnectivityInspector<Long, DefaultWeightedEdge> conn = new ConnectivityInspector<Long, DefaultWeightedEdge>(
					graph);
			final Set<Long> ids = conn.connectedSetOf(flickrId);

			for (final long r : ids) {
				final ScoreDoc sd = lookup(r);
				if (sd != null) {
					// final List<DefaultWeightedEdge> path =
					// DijkstraShortestPath.findPathBetween(graph, flickrId, r);
					//
					// for (final DefaultWeightedEdge pe : path)
					// sd.score += (float) graph.getEdgeWeight(pe);
					// sd.score /= path.size();
					results.add(sd);
				}
			}
		}

		Collections.sort(results, new Comparator<ScoreDoc>() {
			@Override
			public int compare(ScoreDoc o1, ScoreDoc o2) {
				return Float.compare(o2.score, o1.score);
			}
		});

		results = results.subList(0, Math.min(numResults, results.size()));

		return results.toArray(new ScoreDoc[results.size()]);
	}

	private ScoreDoc lookup(long id) throws IOException {
		final Query q = NumericRangeQuery.newLongRange("id", id, id, true, true);
		final ScoreDoc[] docs = meta.search(q, 1).scoreDocs;
		if (docs.length > 0) {
			final ScoreDoc sd = docs[0];

			return sd;
		}
		return null;
	}

	@Override
	public String toString() {
		return "LSHSiftGraphSearcher[expand=" + expand + "]";
	}
}
