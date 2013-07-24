package org.openimaj.tools.clustering;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.SparseIntFV;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.feature.dense.gradient.dsift.PyramidDenseSIFT;
import org.openimaj.image.feature.local.aggregate.BagOfVisualWords;
import org.openimaj.image.feature.local.aggregate.BlockSpatialAggregator;
import org.openimaj.ml.annotation.linear.LiblinearAnnotator.Mode;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.util.pair.IntFloatPair;

import de.bwaldvogel.liblinear.SolverType;

public class PyramidDenseSIFTFeatureExtractorBuilder {
	private int step = 5;
	private int binSize = 7;
	private float magFactor = 6f;
	private int size = 7;
	private float energyThreshold = 0.005f;
	private int blocksX = 2;
	private int blocksY = 2;
	private float phowEnergyThreshold = 0.015f;
	
	public PyramidDenseSIFTFeatureExtractorBuilder(String[] args) throws BuildException {

		for (int i = 0; i < args.length; i++) {
			String[] option = args[i].split("=", 2);
			
			switch (option[0]) {
			case "step":					step = Integer.parseInt(option[1]);					break;
			case "binSize":					binSize = Integer.parseInt(option[1]);				break;
			case "magFactor":				magFactor = Float.parseFloat(option[1]);			break;
			case "size":					size = Integer.parseInt(option[1]);					break;
			case "energyThreshold":			energyThreshold = Float.parseFloat(option[1]);		break;
			case "blocksX":					blocksX = Integer.parseInt(option[1]);				break;
			case "blocksY":					blocksY = Integer.parseInt(option[1]);				break;
			case "phowEnergyThreshold":		phowEnergyThreshold = Float.parseFloat(option[1]);	break;
			default:		throw new BuildException("Unknown option: " + option[0]);
			}
		}
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
