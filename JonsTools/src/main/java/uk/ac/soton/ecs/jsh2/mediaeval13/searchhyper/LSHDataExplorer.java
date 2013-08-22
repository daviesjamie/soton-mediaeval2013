package uk.ac.soton.ecs.jsh2.mediaeval13.searchhyper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.openimaj.util.pair.ObjectDoublePair;
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.googlecode.jatl.Html;

public class LSHDataExplorer {
	private static final String COLLECTION_BASE = "http://homer.ecs.soton.ac.uk/~jsh2/collection/";

	public static LSHDataExplorer INSTANCE = new LSHDataExplorer(new File(
			"/Volumes/My Book/mediaeval-searchhyper/sift1x-lsh-edges.txt"), 10);

	private SimpleWeightedGraph<String, DefaultWeightedEdge> graph;
	private List<String> verticesList;

	private LSHDataExplorer(File file, double minEdgeWeight) {
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

	List<String> randomImages(int n) {
		Collections.shuffle(verticesList);
		return verticesList.subList(0, n);
	}

	List<ObjectDoublePair<String>> search(String path) {
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

	List<ObjectDoublePair<String>> expandedSearch(String query, boolean score) {
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
				final List<DefaultWeightedEdge> path =
						DijkstraShortestPath.findPathBetween(graph, query, r);

				for (final DefaultWeightedEdge pe : path)
					res.second += graph.getEdgeWeight(pe);
				res.second /= path.size();
			} else {
				res.second = 1;
			}
		}

		Collections.sort(results, ObjectDoublePair.SECOND_ITEM_DESCENDING_COMPARATOR);

		return results;
	}

	public static class RandomImages extends ServerResource {
		@Get("html")
		public String getRandom() {
			final StringWriter writer = new StringWriter();

			new Html(writer) {
				{
					html();
					body();
					makeImages();
					endAll();
					done();
				}

				void makeImages() {
					for (final String path : LSHDataExplorer.INSTANCE.randomImages(10)) {
						a().href("search?img=" + path);
						img().src(COLLECTION_BASE + path).width("180").height("101").end();
						end();
					}
				}
			};

			return writer.toString();
		}
	}

	public static class Search extends ServerResource {
		@Get("html")
		public String search() {
			final String img = getQueryValue("img");
			final String expand = getQueryValue("expand");
			final String score = getQueryValue("score");

			final List<ObjectDoublePair<String>> results;
			if (expand == null)
				results = LSHDataExplorer.INSTANCE.search(img);
			else if (score == null)
				results = LSHDataExplorer.INSTANCE.expandedSearch(img, false);
			else
				results = LSHDataExplorer.INSTANCE.expandedSearch(img, true);

			final StringWriter writer = new StringWriter();

			new Html(writer) {
				{
					html();
					body();
					div();
					img().src(COLLECTION_BASE + img).width("180").height("101").end();
					br();
					end();
					makeResults();
					endAll();
					done();
				}

				void makeResults() {
					for (final ObjectDoublePair<String> path : results) {
						div().style("float: left");
						a().href("search?img=" + path.first);
						img().src(COLLECTION_BASE + path.first).width("180").height("101").end();
						end();
						div().text("" + path.second).end();
						end();
					}
				}
			};

			return writer.toString();
		}
	}

	public static void main(String[] args) throws Exception {
		final Component component = new Component();
		component.getServers().add(Protocol.HTTP, 8182);
		component.getDefaultHost().attach("/", RandomImages.class);
		component.getDefaultHost().attach("/search", Search.class);
		component.start();
	}
}
