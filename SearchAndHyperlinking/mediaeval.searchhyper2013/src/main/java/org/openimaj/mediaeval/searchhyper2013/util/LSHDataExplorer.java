package org.openimaj.mediaeval.searchhyper2013.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.openimaj.util.pair.ObjectDoublePair;

/**
 * Represents images as vertices in a graph connected when the number of common 
 * SIFT features (determined by LSH) exceeds a specified threshold.
 * 
 * @author Jon Hare
 *
 */
public class LSHDataExplorer {
	
	private SimpleWeightedGraph<String, DefaultWeightedEdge> graph;
	private List<String> verticesList;

	public LSHDataExplorer(File file, double minEdgeWeight) {
		try {
			graph = loadGraph(file, 10);
			verticesList = new ArrayList<String>(graph.vertexSet());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private SimpleWeightedGraph<String, DefaultWeightedEdge> loadGraph(File file, double minEdgeWeight)
			throws IOException
	{
		final SimpleWeightedGraph<String, DefaultWeightedEdge> graph = new SimpleWeightedGraph<String, DefaultWeightedEdge>(
				DefaultWeightedEdge.class);

		final BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		// int i = 0;
		while ((line = reader.readLine()) != null) {
			final String[] parts = line.split("\\s");

			final String from = parts[0];
			final String to = parts[1];
			final double weight = Double.parseDouble(parts[2]);

			if (weight < minEdgeWeight)
				continue;

			graph.addVertex(to);
			graph.addVertex(from);
			final DefaultWeightedEdge edge = graph.addEdge(from, to);
			if (edge != null)
				graph.setEdgeWeight(edge, weight);

			// if (i++ == 1000)
			// break;
		}
		reader.close();
		return graph;
	}

	public List<String> randomImages(int n) {
		Collections.shuffle(verticesList);
		return verticesList.subList(0, n);
	}

	public List<ObjectDoublePair<String>> search(String path) {
		final Set<DefaultWeightedEdge> edges = graph.edgesOf(path);

		final List<ObjectDoublePair<String>> results = new ArrayList<ObjectDoublePair<String>>();
		for (final DefaultWeightedEdge e : edges) {
			String r = graph.getEdgeSource(e);
			if (r.equals(path))
				r = graph.getEdgeTarget(e);

			results.add(new ObjectDoublePair<String>(r, graph.getEdgeWeight(e)));
		}

		Collections.sort(results, ObjectDoublePair.SECOND_ITEM_DESCENDING_COMPARATOR);

		return results;
	}

	public List<ObjectDoublePair<String>> expandedSearch(String query, boolean score) {
		final ConnectivityInspector<String, DefaultWeightedEdge> conn =
				new ConnectivityInspector<String, DefaultWeightedEdge>(graph);
		final Set<String> ids = conn.connectedSetOf(query);

		final List<ObjectDoublePair<String>> results = new ArrayList<ObjectDoublePair<String>>();
		for (final String r : ids) {
			if (r.equals(query))
				continue;

			final ObjectDoublePair<String> res = new ObjectDoublePair<String>(r, 0);
			results.add(res);

			if (score) {
				res.second = 0;
				
				final List<DefaultWeightedEdge> path =
						DijkstraShortestPath.findPathBetween(graph, query, r);

				for (final DefaultWeightedEdge pe : path)
					res.second = Math.max(res.second, graph.getEdgeWeight(pe));
					//res.second += graph.getEdgeWeight(pe);
				//res.second /= path.size();
			} else {
				res.second = 1;
			}
		}

		Collections.sort(results, ObjectDoublePair.SECOND_ITEM_DESCENDING_COMPARATOR);

		return results;
	}
}
