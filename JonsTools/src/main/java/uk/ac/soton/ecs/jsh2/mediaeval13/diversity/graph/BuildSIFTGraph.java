package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.graph;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.list.MemoryLocalFeatureList;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.io.IOUtils;
import org.openimaj.lsh.functions.DoubleGaussianFactory;
import org.openimaj.lsh.sketch.IntLSHSketcher;
import org.openimaj.util.hash.HashFunction;
import org.openimaj.util.hash.HashFunctionFactory;
import org.openimaj.util.hash.modifier.LSBModifier;
import org.openimaj.util.pair.LongObjectPair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultList;
import cern.jet.random.engine.MersenneTwister;

public class BuildSIFTGraph {
	public static LocalFeatureList<Keypoint> getSIFT1x(ResultItem ri) throws IOException {
		final File f = new File(ri.container.base, "sift1x/" + ri.container.monument + "/" + ri.id + ".sift");

		if (!f.exists()) {
			f.getParentFile().mkdirs();

			final DoGSIFTEngine e = new DoGSIFTEngine();
			e.getOptions().setDoubleInitialImage(false);
			final LocalFeatureList<Keypoint> feats = e.findFeatures(ri.getFImage());
			IOUtils.writeASCII(f, feats);
			return feats;
		} else {
			return MemoryLocalFeatureList.read(f, Keypoint.class);
		}
	}

	public static LocalFeatureList<Keypoint> getSIFT2x(ResultItem ri) throws IOException {
		final File f = new File(ri.container.base, "sift2x/" + ri.container.monument + "/" + ri.id + ".sift");

		if (!f.exists()) {
			f.getParentFile().mkdirs();

			final DoGSIFTEngine e = new DoGSIFTEngine();
			e.getOptions().setDoubleInitialImage(true);
			final LocalFeatureList<Keypoint> feats = e.findFeatures(ri.getFImage());
			IOUtils.writeASCII(f, feats);
			return feats;
		} else {
			return MemoryLocalFeatureList.read(f, Keypoint.class);
		}
	}

	public static class Sketcher {
		private static final int nbits = 128;
		private static final int ndims = 128;
		private static final int seed = 1;
		private static final double w = 6.0;
		final float LOG_BASE = 0.001f;
		private IntLSHSketcher<double[]> sketcher;

		public Sketcher() {
			final MersenneTwister rng = new MersenneTwister(seed);

			final DoubleGaussianFactory gauss = new DoubleGaussianFactory(ndims, rng, w);
			final HashFunctionFactory<double[]> factory = new HashFunctionFactory<double[]>() {
				@Override
				public HashFunction<double[]> create() {
					return new LSBModifier<double[]>(gauss.create());
				}
			};

			sketcher = new IntLSHSketcher<double[]>(factory, nbits);
		}

		public int[] sketch(Keypoint k) {
			return sketcher.createSketch(logScale(k.ivec, LOG_BASE));
		}

		double[] logScale(byte[] v, float l) {
			final double[] dfv = new double[v.length];
			final double s = -Math.log(l);

			for (int i = 0; i < v.length; i++) {
				double d = (v[i] + 128.0) / 256.0;

				if (d < l)
					d = l;
				d = (Math.log(d) + s) / s;
				if (d > 1.0)
					d = 1.0;

				dfv[i] = d;
			}
			return dfv;
		}
	}

	public static void buildGraph(ResultList rl) throws IOException {
		final TLongObjectHashMap<ResultItem> items = new TLongObjectHashMap<ResultItem>();
		final Sketcher sk = new Sketcher();
		final List<TIntObjectMap<TLongSet>> maps = new ArrayList<TIntObjectMap<TLongSet>>();
		for (int i = 0; i < sk.sketcher.arrayLength(); i++)
			maps.add(new TIntObjectHashMap<TLongSet>());

		for (final ResultItem ri : rl) {
			items.put(ri.id, ri);

			final LongObjectPair<LocalFeatureList<Keypoint>> data = LongObjectPair.pair(ri.id, getSIFT2x(ri));

			final long id = data.first;
			for (final Keypoint kpt : data.second) {
				final int[] codes = sk.sketch(kpt);

				for (int i = 0; i < codes.length; i++) {
					final TIntObjectMap<TLongSet> map = maps.get(i);

					if (!map.containsKey(codes[i]))
						map.put(codes[i], new TLongHashSet());
					map.get(codes[i]).add(id);
				}
			}
		}

		final SimpleWeightedGraph<Long, DefaultWeightedEdge> graph = new SimpleWeightedGraph<Long, DefaultWeightedEdge>(
				DefaultWeightedEdge.class);

		for (int i = 0; i < sk.sketcher.arrayLength(); i++) {
			maps.get(i).forEachEntry(new TIntObjectProcedure<TLongSet>() {
				@Override
				public boolean execute(int a, TLongSet b) {
					// filter out hashes with lots of collisions
					if (b.size() < 20) {
						final long[] vals = b.toArray();

						for (int j = 0; j < vals.length; j++) {
							if (!graph.containsVertex(vals[j]))
								graph.addVertex(vals[j]);

							for (int k = j + 1; k < vals.length; k++) {
								if (!graph.containsVertex(vals[k]))
									graph.addVertex(vals[k]);

								DefaultWeightedEdge e = graph.getEdge(vals[j], vals[k]);
								if (e == null)
									e = graph.addEdge(vals[j], vals[k]);

								graph.setEdgeWeight(e, graph.getEdgeWeight(e) + 1);
							}
						}
					}

					return true;
				}
			});
		}

		final List<DefaultWeightedEdge> toRem = new
				ArrayList<DefaultWeightedEdge>();
		for (final DefaultWeightedEdge e : graph.edgeSet()) {
			if (graph.getEdgeWeight(e) < 5)
				toRem.add(e);
		}
		graph.removeAllEdges(toRem);

		final ConnectivityInspector<Long, DefaultWeightedEdge> conn = new
				ConnectivityInspector<Long, DefaultWeightedEdge>(
						graph);
		for (final Set<Long> subgraph : conn.connectedSets()) {
			for (final Long l : subgraph) {
				System.out.format("<img src=\"file://%s\" width='200'/>\n",
						items.get(l).getImageFile());
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
