package org.openimaj.tools.clustering;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.data.DataSource;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.experiment.evaluation.classification.Classifier;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.SparseIntFV;
import org.openimaj.feature.local.data.LocalFeatureListDataSource;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.list.MemoryLocalFeatureList;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.feature.ImageAnalyserFVFeatureExtractor;
import org.openimaj.image.feature.dense.gradient.dsift.ByteDSIFTKeypoint;
import org.openimaj.image.feature.dense.gradient.dsift.DenseSIFT;
import org.openimaj.image.feature.dense.gradient.dsift.FloatDSIFTKeypoint;
import org.openimaj.image.feature.dense.gradient.dsift.PyramidDenseSIFT;
import org.openimaj.image.feature.local.aggregate.BagOfVisualWords;
import org.openimaj.image.feature.local.aggregate.BlockSpatialAggregator;
import org.openimaj.ml.annotation.linear.LiblinearAnnotator;
import org.openimaj.ml.annotation.linear.LiblinearAnnotator.Mode;
import org.openimaj.ml.clustering.ByteCentroidsResult;
import org.openimaj.ml.clustering.DoubleCentroidsResult;
import org.openimaj.ml.clustering.assignment.Assigner;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.kmeans.ByteKMeans;
import org.openimaj.ml.clustering.kmeans.DoubleKMeans;
import org.openimaj.ml.clustering.kmeans.KMeansConfiguration;
import org.openimaj.util.pair.IntFloatPair;

import de.bwaldvogel.liblinear.SolverType;

public class PyramidDenseSIFTKMeansClustererInstanceBuilder implements ClassifierInstanceBuilder<MBFImage> {
	private int step = 5;
	private int binSize = 7;
	private int M = 128;
	private int K = 300;
	private float magFactor = 6f;
	private int size = 7;
	private float energyThreshold = 0.005f;
	private int maxKeypoints = 10000;
	private float C = 1;
	private float eps = 0.0001f;
	private int blocksX = 2;
	private int blocksY = 2;
	private float phowEnergyThreshold = 0.015f;
	
	
	@Override
	public Classifier<String, MBFImage> build(GroupedDataset<String, ListDataset<MBFImage>, MBFImage> developmentSource) {
		DenseSIFT dsift = new DenseSIFT(step, binSize);
		PyramidDenseSIFT<FImage> pdsift = new PyramidDenseSIFT<FImage>(dsift, magFactor, size);
		
		List<LocalFeatureList<ByteDSIFTKeypoint>> keypoints = 
			new ArrayList<LocalFeatureList<ByteDSIFTKeypoint>>();
		
		int count = 1;
		for (MBFImage image : developmentSource) {
			System.out.println("DSIFTing image: " + count + "/" + developmentSource.numInstances());
			
			pdsift.analyseImage(image.flatten());
			LocalFeatureList<ByteDSIFTKeypoint> keypointList = pdsift.getByteKeypoints(energyThreshold);
			
			System.out.println("Keypoints: " + keypointList.size());
			
			keypoints.add(keypointList);
			
			count++;
		}
		
		System.out.println("Got " + keypoints.size() + " keypoints");
		
		if (keypoints.size() > maxKeypoints) {
			System.out.println("Total keypoints exceeded, reducing to " + maxKeypoints);
			
			keypoints = keypoints.subList(0, maxKeypoints);
		}
		
		System.out.println("Creating KD tree ensemble...");
		ByteKMeans km = ByteKMeans.createKDTreeEnsemble(M, K);
		System.out.println("Creating local feature data source...");
		DataSource<byte[]> datasource = new LocalFeatureListDataSource<ByteDSIFTKeypoint, byte[]>(keypoints);
		
		System.out.println("Clustering...");
		ByteCentroidsResult result = km.cluster(datasource);
		
		HardAssigner<byte[], float[], IntFloatPair> assigner = result.defaultHardAssigner();
		
		FeatureExtractor<DoubleFV, MBFImage> extractor = new PHOWExtractor(pdsift, assigner);
		
		System.out.println("Training Liblinear annotator...");
		LiblinearAnnotator<MBFImage, String> ann =
			new LiblinearAnnotator<MBFImage, String>(extractor, Mode.MULTICLASS, SolverType.L2R_L2LOSS_SVC, C, eps);
		ann.train(new ListAnnotatedGroupedDatasetWrapper<MBFImage, String>(developmentSource));
		
		return ann;
	}

	private class PHOWExtractor implements FeatureExtractor<DoubleFV, MBFImage> {
		private PyramidDenseSIFT<FImage> pdsift;
		private HardAssigner<byte[], float[], IntFloatPair> assigner;

		public PHOWExtractor(PyramidDenseSIFT<FImage> pdsift,
				HardAssigner<byte[], float[], IntFloatPair> assigner) {
			this.pdsift = pdsift;
			this.assigner = assigner;
		}

		@Override
		public DoubleFV extractFeature(MBFImage object) {
			pdsift.analyseImage(object.flatten());
			
			BagOfVisualWords<byte[]> bovw = new BagOfVisualWords<byte[]>(assigner);
			
			BlockSpatialAggregator<byte[], SparseIntFV> spatial = new BlockSpatialAggregator<byte[], SparseIntFV>(bovw, blocksX, blocksY);
			
			return spatial.aggregate(pdsift.getByteKeypoints(phowEnergyThreshold), object.getBounds()).normaliseFV();
		}
		
	}

}
