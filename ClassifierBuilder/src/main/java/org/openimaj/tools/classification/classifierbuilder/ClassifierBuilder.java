package org.openimaj.tools.classification.classifierbuilder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.identity.IdentifiableObject;
import org.openimaj.experiment.dataset.split.GroupedRandomSplitter;
import org.openimaj.experiment.evaluation.classification.ClassificationEvaluator;
import org.openimaj.experiment.evaluation.classification.ClassificationResult;
import org.openimaj.experiment.evaluation.classification.analysers.confusionmatrix.CMAnalyser;
import org.openimaj.experiment.evaluation.classification.analysers.confusionmatrix.CMResult;
import org.openimaj.feature.DiskCachingFeatureExtractor;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.IdentifiableObjectUnwrappingFeatureExtractor;
import org.openimaj.feature.MessageDigestIdentifiableWrappingFeatureExtractor;
import org.openimaj.image.MBFImage;
import org.openimaj.io.IOUtils;
import org.openimaj.ml.annotation.linear.LiblinearAnnotator;
import org.openimaj.ml.annotation.linear.LiblinearAnnotator.Mode;
import org.openimaj.tools.RequiredArgumentException;
import org.openimaj.tools.UserInputException;
import org.openimaj.util.data.dataset.BuildException;
import org.openimaj.util.data.dataset.DatasetBuilderUtilities;

import de.bwaldvogel.liblinear.SolverType;

public class ClassifierBuilder {

	/**
	 * A tool for automatically training classifiers.
	 * 
	 * @param args
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws BuildException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static void main(String[] args) throws ParseException, IOException, BuildException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, NoSuchAlgorithmException {
		Options options = new Options();
		options.addOption("A", true, "LiblinearAnnotator configuration");
		options.addOption("D", true, "dataset profile");
		options.addOption("F", true, "FeatureExtractor object file");
		options.addOption("f", true, "output file for FeatureExtractor cache");
		options.addOption("t", true, "number of instances in training set");
		options.addOption("v", true, "number of instances in validation set");
		options.addOption("s", true, "number of instances in testing set");
		options.addOption("o", true, "output file for Classifier object");
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		
		String annotatorConfig = cmd.getOptionValue("A");
		String datasetProfile = cmd.getOptionValue("D");
		String featureExtractorFile = cmd.getOptionValue("F");
		String featureExtractorCacheFile = cmd.getOptionValue("f");
		String trainingSetSize = cmd.getOptionValue("t");
		String validationSetSize = cmd.getOptionValue("v");
		String testingSetSize = cmd.getOptionValue("s");
		String outFile = cmd.getOptionValue("o");
		
		LiblinearAnnotator<MBFImage, String> annotator;
		GroupedDataset<String, ListDataset<MBFImage>, MBFImage> dataSource;
		FeatureExtractor<DoubleFV, MBFImage> featureExtractor;
		int trainingSize;
		int validationSize;
		int testingSize;
		File out;
		
		try {
			if (datasetProfile == null) {
				throw new RequiredArgumentException("D");
			}
			if (featureExtractorFile == null) {
				throw new RequiredArgumentException("F");
			}
			if (featureExtractorCacheFile == null) {
				throw new RequiredArgumentException("f");
			}
			if (trainingSetSize == null) {
				throw new RequiredArgumentException("t");
			}
			if (validationSetSize == null) {
				throw new RequiredArgumentException("v");
			}
			if (testingSetSize == null) {
				throw new RequiredArgumentException("s");
			}
			if (outFile == null) {
				throw new RequiredArgumentException("o");
			}
			
			featureExtractor = buildFeatureExtractor(featureExtractorFile, featureExtractorCacheFile);
			annotator = buildAnnotator(annotatorConfig, featureExtractor);
			trainingSize = Integer.parseInt(trainingSetSize);
			validationSize = Integer.parseInt(validationSetSize);
			testingSize = Integer.parseInt(testingSetSize);
			dataSource = DatasetBuilderUtilities.buildMBFImageDataset(datasetProfile, trainingSize + validationSize + testingSize);
			out = Paths.get(outFile).toFile();
			
		} catch (UserInputException e) {
			System.err.println(e.getMessage());
			
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("ClassifierBuilder", options);
			
			return;
		}

		System.out.println("Building training, validation, and testing datasets...");
		GroupedRandomSplitter<String, MBFImage> splitter = 
			new GroupedRandomSplitter<String, MBFImage>(dataSource, trainingSize, validationSize, testingSize);
		
		System.out.println("Training annotator with training set...");
		annotator.train(splitter.getTrainingDataset());
		
		System.out.println("Testing annotator against testing set...");
		ClassificationEvaluator<CMResult<String>, String, MBFImage> eval = 
			new ClassificationEvaluator<CMResult<String>, String, MBFImage>(
				annotator, splitter.getTestDataset(), new CMAnalyser<MBFImage, String>(CMAnalyser.Strategy.SINGLE));
		Map<MBFImage, ClassificationResult<String>> guesses = eval.evaluate();
		CMResult<String> result = eval.analyse(guesses);
		System.out.println(result);
		
		System.out.println("Writing to file...");
		IOUtils.writeToFile(annotator, out);
	}

	private static LiblinearAnnotator<MBFImage, String> buildAnnotator(
			String annotatorConfig, FeatureExtractor<DoubleFV, MBFImage> featureExtractor) throws BuildException {
		
		Mode mode = Mode.MULTICLASS;
		SolverType solverType = SolverType.L2R_L2LOSS_SVC;
		float C = 1.0f;
		float eps = 0.00001f;
		
		if (annotatorConfig != null) {
			for (String setting : annotatorConfig.split(",")) {
				String[] option = setting.split("=", 2);
				
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
	
				case "C":						C = Float.parseFloat(option[1]);					break;
				case "eps":						eps = Float.parseFloat(option[1]);					break;
				default:		throw new BuildException("Unknown annotator option: " + option[0]);
				}
			}
		}
		
		return new LiblinearAnnotator<MBFImage, String>(featureExtractor, mode, solverType, C, eps);
	}

	private static FeatureExtractor<DoubleFV, MBFImage> buildFeatureExtractor(
			String featureExtractorFile, String featureExtractorCacheFile) throws IOException, NoSuchAlgorithmException {

		FeatureExtractor<DoubleFV, MBFImage> featureExtractor =
				IOUtils.readFromFile(new File(featureExtractorFile));

		IdentifiableObjectUnwrappingFeatureExtractor<DoubleFV, MBFImage> unwrappingFeatureExtractor  = 
			new IdentifiableObjectUnwrappingFeatureExtractor<DoubleFV, MBFImage>(featureExtractor);
		
		DiskCachingFeatureExtractor<DoubleFV, IdentifiableObject<MBFImage>> diskCachingFeatureExtractor = 
			new DiskCachingFeatureExtractor<DoubleFV, IdentifiableObject<MBFImage>>(
					new File(featureExtractorCacheFile),
					unwrappingFeatureExtractor);
		
		MessageDigestIdentifiableWrappingFeatureExtractor<DoubleFV, MBFImage> wrappingFeatureExtractor = 
			new MessageDigestIdentifiableWrappingFeatureExtractor<DoubleFV, MBFImage>(
					MessageDigest.getInstance("MD5"),
					diskCachingFeatureExtractor);
		
		return wrappingFeatureExtractor;
	}

}
