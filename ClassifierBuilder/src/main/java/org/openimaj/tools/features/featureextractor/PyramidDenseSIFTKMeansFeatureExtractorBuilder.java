package org.openimaj.tools.features.featureextractor;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.data.DataSource;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.SparseIntFV;
import org.openimaj.feature.local.data.LocalFeatureListDataSource;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.feature.dense.gradient.dsift.ByteDSIFTKeypoint;
import org.openimaj.image.feature.dense.gradient.dsift.DenseSIFT;
import org.openimaj.image.feature.dense.gradient.dsift.PyramidDenseSIFT;
import org.openimaj.image.feature.local.aggregate.BagOfVisualWords;
import org.openimaj.image.feature.local.aggregate.BlockSpatialAggregator;
import org.openimaj.ml.clustering.ByteCentroidsResult;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.kmeans.ByteKMeans;
import org.openimaj.util.data.dataset.BuildException;
import org.openimaj.util.pair.IntFloatPair;

public class PyramidDenseSIFTKMeansFeatureExtractorBuilder implements FeatureExtractorInstanceBuilder<DoubleFV, MBFImage> {
	private int step = 5;
	private int binSize = 7;
	private int M = 128;
	private int K = 300;
	private float magFactor = 6f;
	private int size = 7;
	private float energyThreshold = 0.005f;
	private int maxKeypoints = 10000;
	private int blocksX = 2;
	private int blocksY = 2;
	private float phowEnergyThreshold = 0.015f;
	
	public PyramidDenseSIFTKMeansFeatureExtractorBuilder(String[] args) throws BuildException {
		
		if (args == null) {
			return;
		}
		
		for (int i = 0; i < args.length; i++) {
			String[] option = args[i].split("=", 2);
			
			switch (option[0]) {
			case "step":				step = Integer.parseInt(option[1]);					break;
			case "binSize":				binSize = Integer.parseInt(option[1]);				break;
			case "M":					M = Integer.parseInt(option[1]);					break;
			case "K":					K = Integer.parseInt(option[1]);					break;
			case "magFactor":			magFactor = Float.parseFloat(option[1]);			break;
			case "size":				size = Integer.parseInt(option[1]);					break;
			case "energyThreshold":		energyThreshold = Float.parseFloat(option[1]);		break;
			case "maxKeypoints":		maxKeypoints = Integer.parseInt(option[1]);			break;
			case "blocksX":				blocksX = Integer.parseInt(option[1]);				break;
			case "blocksY":				blocksY = Integer.parseInt(option[1]);				break;
			case "phowEnergyThreshold":	phowEnergyThreshold = Float.parseFloat(option[1]);	break;
			default:	throw new BuildException("Unknown option: " + option[0]);
			}
		}
	}
	
	@Override
	public FeatureExtractor<DoubleFV, MBFImage> build(Dataset<MBFImage> developmentSource) {
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
		
		PHOWExtractor extractor =
			new PHOWExtractor(pdsift, assigner, blocksX, blocksY, phowEnergyThreshold);
		
		return extractor;
	}

	private static class PHOWExtractor implements FeatureExtractor<DoubleFV, MBFImage> {
		private PyramidDenseSIFT<FImage> pdsift;
		private HardAssigner<byte[], float[], IntFloatPair> assigner;
		private int blocksX;
		private int blocksY;
		private float phowEnergyThreshold;

		public PHOWExtractor(PyramidDenseSIFT<FImage> pdsift,
				HardAssigner<byte[], float[], IntFloatPair> assigner,
				int blocksX, int blocksY, float phowEnergyThreshold) {
			this.pdsift = pdsift;
			this.assigner = assigner;
			this.blocksX = blocksX;
			this.blocksY = blocksY;
			this.phowEnergyThreshold = phowEnergyThreshold;
		}

		@Override
		public DoubleFV extractFeature(MBFImage object) {
			pdsift.analyseImage(object.flatten());
			
			BagOfVisualWords<byte[]> bovw = new BagOfVisualWords<byte[]>(assigner);
			
			BlockSpatialAggregator<byte[], SparseIntFV> spatial = new BlockSpatialAggregator<byte[], SparseIntFV>(bovw, blocksX, blocksY);
			
			DoubleFV feature = spatial.aggregate(pdsift.getByteKeypoints(phowEnergyThreshold), object.getBounds()).normaliseFV();
			
			return feature;
		}
		
	}

}
