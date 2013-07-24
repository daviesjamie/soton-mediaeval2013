package org.openimaj.tools.clustering;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.data.DataSource;
import org.openimaj.data.DoubleArrayBackedDataSource;
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
import org.openimaj.util.pair.IntDoublePair;
import org.openimaj.util.pair.IntFloatPair;

import de.bwaldvogel.liblinear.SolverType;

public class KMeansClustererInstanceBuilder<FEATURE extends DoubleFV, DATA> implements ClassifierInstanceBuilder<FEATURE, DATA> {
	
	private int M = 128;
	private int K = 300;
	private int maxFeatures = 10000;
	private float C = 1;
	private float eps = 0.0001f;
	private Mode mode = Mode.MULTICLASS;
	private SolverType solverType = SolverType.L2R_L2LOSS_SVC;
	
	public KMeansClustererInstanceBuilder() {
		super();
	}
	
	public KMeansClustererInstanceBuilder(String[] args) throws BuildException {

		for (int i = 0; i < args.length; i++) {
			String[] option = args[i].split("=", 2);
			
			switch (option[0]) {
			case "mode":
				switch(option[1]) {
				case "multiclass":			mode = Mode.MULTICLASS;							break;
				case "multilabel":			mode = Mode.MULTILABEL;							break;
				}																			break;
			case "solverType":
				switch(option[1]) {
				case "L2R_LR":				solverType = SolverType.L2R_LR;						break;
				case "L2R_L2LOSS_SVC_DUAL":	solverType = SolverType.L2R_L2LOSS_SVC_DUAL;		break;
				case "L2R_L2LOSS_SVC":		solverType = SolverType.L2R_L2LOSS_SVC;				break;
				case "L2R_L1LOSS_SVC_DUAL":	solverType = SolverType.L2R_L1LOSS_SVC_DUAL;		break;
				case "MCSVM_CS":			solverType = SolverType.MCSVM_CS;					break;
				case "L1R_L2LOSS_SVC":		solverType = SolverType.L1R_L2LOSS_SVC;				break;
				case "L1R_LR":				solverType = SolverType.L1R_LR;						break;
				case "L2R_LR_DUAL":			solverType = SolverType.L2R_LR_DUAL;				break;
				case "L2R_L2LOSS_SVR":		solverType = SolverType.L2R_L2LOSS_SVR;				break;
				case "L2R_L2LOSS_SVR_DUAL":	solverType = SolverType.L2R_L2LOSS_SVR_DUAL;		break;
				case "L2R_L1LOSS_SVR_DUAL":	solverType = SolverType.L2R_L1LOSS_SVR_DUAL;		break;
				}																				break;
			case "M":						M = Integer.parseInt(option[1]);					break;
			case "K":						K = Integer.parseInt(option[1]);					break;
			case "maxKeypoints":			maxFeatures = Integer.parseInt(option[1]);			break;
			case "C":						C = Float.parseFloat(option[1]);					break;
			case "eps":						eps = Float.parseFloat(option[1]);					break;
			default:		throw new BuildException("Unknown option: " + option[0]);
			}
		}
	}
	
	
	@Override
	public Classifier<String, DATA> build(
			GroupedDataset<String, ListDataset<DATA>, DATA> trainingSource,
			FeatureExtractor<FEATURE, DATA> featureExtractor) {		
		
		List<FEATURE> features = new ArrayList<FEATURE>();
		
		int count = 1;
		for (DATA object : trainingSource) {
			System.out.println("Extracting features: " + count + "/" + trainingSource.numInstances());
			
			features.add(featureExtractor.extractFeature(object));
			
			count++;
		}
		
		if (features.size() > maxFeatures) {
			System.out.println("Total features exceeded, reducing to " + maxFeatures);
			
			features = features.subList(0, maxFeatures);
		}
		
		System.out.println("Creating KD tree ensemble...");
		DoubleKMeans km = DoubleKMeans.createKDTreeEnsemble(M, K);
		
		System.out.println("Creating local feature data source...");
		double[][] data = new double[features.size()][features.get(0).length()];
		
		for (int i = 0; i < data.length; i++) {
			data[i] = features.get(i).asDoubleVector();
		}
		
		DataSource<double[]> datasource = new DoubleArrayBackedDataSource(data);
		
		System.out.println("Clustering...");
		DoubleCentroidsResult result = km.cluster(datasource);
		
		HardAssigner<double[], double[], IntDoublePair> assigner = result.defaultHardAssigner();

		System.out.println("Training Liblinear annotator...");
		LiblinearAnnotator<DATA, String> ann =
			new LiblinearAnnotator<DATA, String>(featureExtractor, mode, solverType, C, eps);
		ann.train(new ListAnnotatedGroupedDatasetWrapper<DATA, String>(trainingSource));
		
		return ann;
	}

	private static class BoWSpatialAggregatorExtractor<FEATURE extends DoubleFV, DATA> implements FeatureExtractor<FEATURE, DATA> {

		@Override
		public FEATURE extractFeature(DATA object) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
