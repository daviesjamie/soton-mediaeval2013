package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.extractors;

import java.io.File;
import java.io.IOException;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.SparseIntFV;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.feature.local.aggregate.BagOfVisualWords;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.io.IOUtils;
import org.openimaj.ml.clustering.ByteCentroidsResult;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.assignment.hard.KDTreeByteEuclideanAssigner;
import org.openimaj.util.pair.IntFloatPair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.graph.BuildSIFTGraph;

public class SIFTBOVW implements FeatureExtractor<DoubleFV, ResultItem> {
	BagOfVisualWords<byte[]> bw;

	public SIFTBOVW() {
		HardAssigner<byte[], float[], IntFloatPair> assigner;
		try {
			assigner = new KDTreeByteEuclideanAssigner(IOUtils
					.read(new File("/Users/jon/Desktop/mirflickr-1000000-sift-fastkmeans-new.idx"),
							ByteCentroidsResult.class));
			bw = new BagOfVisualWords<byte[]>(assigner);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public DoubleFV extractFeature(ResultItem object) {
		try {
			final File f = new File(object.container.base, "sift1x-bovw/" + object.container.monument + "/" + object.id
					+ ".bovw");

			if (!f.exists()) {
				final LocalFeatureList<Keypoint> sift = BuildSIFTGraph.getSIFT1x(object);
				final SparseIntFV fv = bw.aggregate(sift);

				f.getParentFile().mkdirs();
				IOUtils.writeBinary(f, fv);
				return fv.normaliseFV();
			} else {
				return IOUtils.read(f, SparseIntFV.class).normaliseFV();
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
}
