package uk.ac.soton.ecs.jsh2.mediaeval13.crowd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.data.DataSource;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.experiment.dataset.sampling.UniformRandomisedSampler;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.SparseIntFV;
import org.openimaj.feature.local.data.LocalFeatureListDataSource;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.feature.dense.gradient.dsift.ByteDSIFTKeypoint;
import org.openimaj.image.feature.dense.gradient.dsift.DenseSIFT;
import org.openimaj.image.feature.dense.gradient.dsift.PyramidDenseSIFT;
import org.openimaj.image.feature.local.aggregate.BagOfVisualWords;
import org.openimaj.image.feature.local.aggregate.PyramidSpatialAggregator;
import org.openimaj.io.IOUtils;
import org.openimaj.ml.clustering.ByteCentroidsResult;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.kmeans.ByteKMeans;
import org.openimaj.util.function.Operation;
import org.openimaj.util.pair.IntFloatPair;
import org.openimaj.util.parallel.Parallel;

public class PHOWExtractor {
	public static void main(String[] args) throws IOException {
		System.out.println("Load dataset and take a sample");
		final VFSListDataset<FImage> dataset = new VFSListDataset<FImage>(
				"/Volumes/My Book/mediaeval-crowd/Fashion10000/Photos", ImageUtilities.FIMAGE_READER);

		final File base = new File("/Volumes/My Book/mediaeval-crowd/Fashion10000/Features");
		final File vocabFile = new File(base, "siftvoc300.bin");

		System.out.println("Construct the base feature extractor");
		final DenseSIFT dsift = new DenseSIFT(5, 7);
		final PyramidDenseSIFT<FImage> pdsift = new PyramidDenseSIFT<FImage>(dsift, 6f, 7);

		System.out.println("Learn a vocabulary");
		final HardAssigner<byte[], float[], IntFloatPair> assigner;
		if (vocabFile.exists()) {
			assigner = IOUtils.readFromFile(vocabFile);
		} else {
			assigner = trainQuantiser(new UniformRandomisedSampler<FImage>(
					30).sample(dataset), pdsift);
			vocabFile.getParentFile().mkdirs();
			IOUtils.writeToFile(assigner, vocabFile);
		}

		final FeatureExtractor<DoubleFV, FImage> extractor = new SpPHOWExtractorImplementation(assigner);
		Parallel.forIndex(0, dataset.size(), 1, new Operation<Integer>() {
			@Override
			public void perform(Integer index) {
				try {
					final FImage img = dataset.get(index);
					final String fn = dataset.getID(index);
					System.out.println(fn);
					final DoubleFV feature = extractor.extractFeature(img);

					final File file = new File(base, fn);
					file.getParentFile().mkdirs();

					IOUtils.writeASCII(file, feature);
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	private static final class SpPHOWExtractorImplementation implements FeatureExtractor<DoubleFV, FImage> {
		HardAssigner<byte[], float[], IntFloatPair> assigner;

		public SpPHOWExtractorImplementation(HardAssigner<byte[], float[], IntFloatPair> assigner)
		{
			this.assigner = assigner;
		}

		@Override
		public DoubleFV extractFeature(FImage image) {
			final DenseSIFT dsift = new DenseSIFT(5, 7);
			final PyramidDenseSIFT<FImage> pdsift = new PyramidDenseSIFT<FImage>(dsift, 6f, 7);

			pdsift.analyseImage(image);

			final BagOfVisualWords<byte[]> bovw = new BagOfVisualWords<byte[]>(assigner);

			// final BlockSpatialAggregator<byte[], SparseIntFV> spatial = new
			// BlockSpatialAggregator<byte[], SparseIntFV>(
			// bovw, 2, 2);

			final PyramidSpatialAggregator<byte[], SparseIntFV> spatial =
					new PyramidSpatialAggregator<byte[], SparseIntFV>(bovw, 2, 4);

			return spatial.aggregate(pdsift.getByteKeypoints(0.015f), image.getBounds()).normaliseFV();
		}
	}

	private static HardAssigner<byte[], float[], IntFloatPair> trainQuantiser(
			ListDataset<FImage> sample, PyramidDenseSIFT<FImage> pdsift)
	{
		List<LocalFeatureList<ByteDSIFTKeypoint>> allkeys = new ArrayList<LocalFeatureList<ByteDSIFTKeypoint>>();

		for (final FImage img : sample) {
			pdsift.analyseImage(img);
			allkeys.add(pdsift.getByteKeypoints(0.005f));
		}

		if (allkeys.size() > 10000)
			allkeys = allkeys.subList(0, 10000);

		final ByteKMeans km = ByteKMeans.createKDTreeEnsemble(300);
		final DataSource<byte[]> datasource = new LocalFeatureListDataSource<ByteDSIFTKeypoint, byte[]>(allkeys);
		final ByteCentroidsResult result = km.cluster(datasource);

		return result.defaultHardAssigner();
	}
}
