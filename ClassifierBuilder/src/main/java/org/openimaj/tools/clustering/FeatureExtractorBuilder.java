package org.openimaj.tools.clustering;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.experiment.dataset.split.GroupedRandomSplitter;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.image.MBFImage;
import org.openimaj.io.FileUtils;
import org.openimaj.io.IOUtils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

public class FeatureExtractorBuilder {
	
	/**
	 * A tool for automatically building feature extractors.
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
		options.addOption("F", true, "feature extractor profile");
		options.addOption("D", true, "dataset profile");
		options.addOption("t", true, "number of instances in training set");
		options.addOption("o", true, "output file for FeatureExtractor object");
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		
		String featureExtractorProfile = cmd.getOptionValue("F");
		String datasetProfile = cmd.getOptionValue("D");
		String trainingSetSize = cmd.getOptionValue("t");
		String outFile = cmd.getOptionValue("o");
		
		FeatureExtractorInstanceBuilder<DoubleFV, MBFImage> featureExtractorBuilder;
		GroupedDataset<String, ListDataset<MBFImage>, MBFImage> dataSource;
		int trainingSize;
		File out;
		
		try {
			if (featureExtractorProfile == null) {
				throw new RequiredArgumentException("F");
			}
			if (datasetProfile == null) {
				throw new RequiredArgumentException("D");
			}
			if (trainingSetSize == null) {
				throw new RequiredArgumentException("t");
			}
			if (outFile == null) {
				throw new RequiredArgumentException("o");
			}
			
			featureExtractorBuilder = establishFeatureExtractor(featureExtractorProfile);
			trainingSize = Integer.parseInt(trainingSetSize);
			dataSource = DatasetBuilderUtilities.buildMBFImageDataset(datasetProfile, trainingSize);
			out = Paths.get(outFile).toFile();
			
		} catch (UserInputException e) {
			System.err.println(e.getMessage());
			return;
		}

		System.out.println("Building training dataset...");
		GroupedRandomSplitter<String, MBFImage> splitter = 
			new GroupedRandomSplitter<String, MBFImage>(dataSource, trainingSize, 0, 0);
		
		System.out.println("Building feature extractor...");
		FeatureExtractor<DoubleFV, MBFImage> featureExtractor = 
			featureExtractorBuilder.build(splitter.getTrainingDataset());
		
		System.out.println("Writing to file...");
		IOUtils.writeToFile(featureExtractor, out);
		/*
		Kryo kryo = new Kryo();
		kryo.writeClassAndObject(new Output(new FileOutputStream(out)), featureExtractor);
		*/
	}

	private static FeatureExtractorInstanceBuilder<DoubleFV, MBFImage> establishFeatureExtractor(
			String featureExtractorProfile) throws BuildException {
		String[] split = featureExtractorProfile.split(":", 2);
		
		String[] args = null;
		
		if (split.length == 2) {
			args = split[1].split(",");
		}
		
		switch(split[0]) {
		case "PyramidDenseSIFTKMeans":		return new PyramidDenseSIFTKMeansFeatureExtractorBuilder(args);
		default:		throw new BuildException("Unknown feature extractor profile: " + split[0]);
		}
	}

	

}
