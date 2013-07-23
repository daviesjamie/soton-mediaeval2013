package org.openimaj.tools.clustering;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jfree.io.FileUtilities;
import org.openimaj.data.DataSource;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListBackedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.experiment.dataset.split.GroupedRandomSplitter;
import org.openimaj.experiment.evaluation.classification.ClassificationEvaluator;
import org.openimaj.experiment.evaluation.classification.ClassificationResult;
import org.openimaj.experiment.evaluation.classification.Classifier;
import org.openimaj.experiment.evaluation.classification.analysers.confusionmatrix.CMAnalyser;
import org.openimaj.experiment.evaluation.classification.analysers.confusionmatrix.CMResult;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101.Record;
import org.openimaj.io.FileUtils;
import org.openimaj.ml.clustering.assignment.Assigner;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

public class ClassifierBuilder {
	private static final Map<String, DatasetBuilder<GroupedDataset<String, ListDataset<MBFImage>, MBFImage>>>
		DATASETS = new HashMap<String, DatasetBuilder<GroupedDataset<String, ListDataset<MBFImage>, MBFImage>>>();
	
	private static final Map<String, Class<? extends ClassifierInstanceBuilder<MBFImage>>>
		CLASSIFIERS = new HashMap<String, Class<? extends ClassifierInstanceBuilder<MBFImage>>>();
	
	private static void init() {
		DATASETS.put("Caltech101", new Caltech101DatasetBuilder());
		DATASETS.put("Flickr", new FlickrImageDatasetBuilder());
		
		CLASSIFIERS.put("PyramidDenseSIFTKMeans", PyramidDenseSIFTKMeansClustererInstanceBuilder.class);
	}
	
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
	 */
	public static void main(String[] args) throws ParseException, IOException, BuildException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		init();
		
		Options options = new Options();
		options.addOption("P", true, "classifier profile");
		options.addOption("D", true, "dataset profile");
		options.addOption("t", true, "number of instances in training set");
		options.addOption("v", true, "number of instances in validation set");
		options.addOption("s", true, "number of instances in testing set");
		options.addOption("o", true, "output file for Classifier object");
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		
		String clustererProfile = cmd.getOptionValue("P");
		String datasetProfile = cmd.getOptionValue("D");
		String trainingSetSize = cmd.getOptionValue("t");
		String validationSetSize = cmd.getOptionValue("v");
		String testingSetSize = cmd.getOptionValue("s");
		String outFile = cmd.getOptionValue("o");
		
		ClassifierInstanceBuilder<MBFImage> classifierBuilder;
		GroupedDataset<String, ListDataset<MBFImage>, MBFImage> dataSource;
		int trainingSize;
		int validationSize;
		int testingSize;
		File out;
		
		try {
			if (clustererProfile == null) {
				throw new RequiredArgumentException("P");
			}
			if (datasetProfile == null) {
				throw new RequiredArgumentException("D");
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
			
			classifierBuilder = establishClustererBuilder(clustererProfile);
			dataSource = establishDataset(datasetProfile);
			trainingSize = Integer.parseInt(trainingSetSize);
			validationSize = Integer.parseInt(validationSetSize);
			testingSize = Integer.parseInt(testingSetSize);
			out = Paths.get(outFile).toFile();
			
		} catch (UserInputException e) {
			System.err.println(e.getMessage());
			return;
		}

		System.out.println("Building training, validation, and testing datasets...");
		GroupedRandomSplitter<String, MBFImage> splitter = 
			new GroupedRandomSplitter<String, MBFImage>(dataSource, trainingSize, validationSize, testingSize);
		
		System.out.println("Building classifier with training set...");
		Classifier<String, MBFImage> classifier = classifierBuilder.build(splitter.getTrainingDataset());
		
		System.out.println("Testing classifier against testing set...");
		ClassificationEvaluator<CMResult<String>, String, MBFImage> eval = 
			new ClassificationEvaluator<CMResult<String>, String, MBFImage>(
				classifier, splitter.getTestDataset(), new CMAnalyser<MBFImage, String>(CMAnalyser.Strategy.SINGLE));
		Map<MBFImage, ClassificationResult<String>> guesses = eval.evaluate();
		CMResult<String> result = eval.analyse(guesses);
		System.out.println(result);
		
		System.out.println("Writing to file...");
		Kryo kryo = new Kryo();
		kryo.writeObject(new Output(new FileOutputStream(outFile)), classifier);
	}

	/**
	 * <type>:<args> as in sourceString.
	 * 
	 * @return
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws InvalidClustererException 
	 */
	private static ClassifierInstanceBuilder<MBFImage> establishClustererBuilder(String clustererType) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, InvalidClustererException {
		String[] components = clustererType.split(":", 2);
		
		if (CLASSIFIERS.containsKey(components[0])) {
			System.out.print("Determined profile: ");
			
			if (components.length == 1) {
				System.out.println(components[0]);
				
				return CLASSIFIERS.get(components[0]).getConstructor().newInstance();
			} else {
				System.out.println(components[0] + " with arguments: " + components[1]);
				
				String[] args = components[1].split(",");
				return CLASSIFIERS.get(components[0]).getConstructor(String[].class).newInstance((Object) args);
			}
		} else {
			throw new InvalidClustererException(components[0]);
		}
	}

	/**
	 * sourceString should be a semicolon (;) delimited list of one or more 
	 * dataset specifiers, where each specifier is of the form <type>:<args>, 
	 * where <type> defines the DatasetBuilder to pass <args> to, and where 
	 * <args> is a comma (,) delimited list of arguments, which may be a single
	 * value or a <key>=<value> pair. For example:
	 * 
	 * Caltech101:mode=exclude,flamingo,faces
	 * 
	 * is a valid sourceString.
	 * 
	 * @throws BuildException 
	 */
	private static GroupedDataset<String, ListDataset<MBFImage>, MBFImage> establishDataset(String sourceString) throws IOException, InvalidDatasetException, BuildException {
		System.out.println("Aggregating datasets...");
		
		// Datasets are semicolon-separated.
		String[] subSets = sourceString.split(";");
		
		AggregateGroupedDataset<String, ListDataset<MBFImage>, MBFImage> dataset =
			new AggregateGroupedDataset<String, ListDataset<MBFImage>, MBFImage>();

		for (String subSet : subSets) {
			String[] components = subSet.split(":", 2);
			
			DatasetBuilder<GroupedDataset<String, ListDataset<MBFImage>, MBFImage>> builder = DATASETS.get(components[0]);
			
			if (builder == null) {
				throw new InvalidDatasetException(components[0]);
			}
			
			System.out.print("Adding dataset: " + components[0]);
			
			String[] args = null;
			if (components.length == 2) {
				System.out.println(" with arguments: " + components[1]);
				
				args = components[1].split(",");
			} else {
				System.out.println();
			}
			
			GroupedDataset<String, ListDataset<MBFImage>, MBFImage> subDataset = 
					builder.build(args);
			
			dataset.addDataset(subDataset);
		}
		
		GroupedDataset<String, AggregateListDataset<MBFImage>, MBFImage> cast1 = dataset;
		GroupedDataset<String, ? extends ListDataset<MBFImage>, MBFImage> cast2 = cast1;
		GroupedDataset<String, ListDataset<MBFImage>, MBFImage> cast3 = (GroupedDataset<String, ListDataset<MBFImage>, MBFImage>) cast2;
		
		return cast3;
	}

}
