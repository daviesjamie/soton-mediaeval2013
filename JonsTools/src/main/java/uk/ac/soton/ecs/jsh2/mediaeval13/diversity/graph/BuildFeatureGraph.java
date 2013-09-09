package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.graph;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.pixel.statistics.BlockHistogramModel;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultList;

public class BuildFeatureGraph {
	static Map<ResultItem, DoubleFV> hists = new HashMap<ResultItem, DoubleFV>();

	public static DoubleFV getHist(ResultItem ri) {
		if (!hists.containsKey(ri)) {
			final BlockHistogramModel hm = new BlockHistogramModel(1, 1, 4, 4, 4);
			hm.estimateModel(Transforms.RGB_TO_HSV(ri.getMBFImage().normalise()));
			hists.put(ri, hm.toSingleHistogram().normaliseFV());
		}
		return hists.get(ri);
	}

	public static void buildGraph(ResultList rl) throws IOException {
		final TLongObjectHashMap<ResultItem> items = new TLongObjectHashMap<ResultItem>();

		final SimpleWeightedGraph<Long, DefaultWeightedEdge> graph = new SimpleWeightedGraph<Long, DefaultWeightedEdge>(
				DefaultWeightedEdge.class);

		final FImage img = new FImage(rl.size(), rl.size());
		for (int i = 0; i < rl.size(); i++) {
			items.put(rl.get(i).id, rl.get(i));

			if (!graph.containsVertex(rl.get(i).id))
				graph.addVertex(rl.get(i).id);

			for (int j = i + 1; j < rl.size(); j++) {
				if (!graph.containsVertex(rl.get(j).id))
					graph.addVertex(rl.get(j).id);

				final DefaultWeightedEdge e = graph.addEdge(rl.get(i).id, rl.get(j).id);
				// final DoubleFV h1 = getHist(rl.get(i));
				// final DoubleFV h2 = getHist(rl.get(j));
				final DoubleFV h1 = rl.get(i).getCN3x3();
				final DoubleFV h2 = rl.get(j).getCN3x3();
				final double dist = DoubleFVComparison.EUCLIDEAN.compare(h1, h2);
				// System.out.println(dist);
				graph.setEdgeWeight(e, dist);

				img.pixels[i][j] = img.pixels[j][i] = (float) DoubleFVComparison.COSINE_SIM.compare(h1, h2);
				img.pixels[i][i] = 1;
				img.pixels[j][j] = 1;
			}
		}

		DisplayUtilities.display(img.normalise());

		final List<DefaultWeightedEdge> toRem = new ArrayList<DefaultWeightedEdge>();
		for (final DefaultWeightedEdge e : graph.edgeSet()) {
			if (graph.getEdgeWeight(e) > 1.0)
				toRem.add(e);
		}
		graph.removeAllEdges(toRem);

		final ConnectivityInspector<Long, DefaultWeightedEdge> conn = new ConnectivityInspector<Long, DefaultWeightedEdge>(
				graph);
		for (final Set<Long> subgraph : conn.connectedSets()) {
			for (final Long l : subgraph) {
				System.out.format("<img src=\"file://%s\" width='200'/>\n", items.get(l).getImageFile());
			}

			System.out.println("<hr/>");
		}
	}

	public static void main(String[] args) throws Exception {
		final ResultList rl = new ResultList(new File(
				"/Users/jon/Data/mediaeval/diversity/devset/keywordsGPS/devsetkeywordsGPS/xml/Angel of the North.xml"));

		buildGraph(rl);
	}
}
