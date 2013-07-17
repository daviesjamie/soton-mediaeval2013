package org.openimaj.tools.clustering;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openimaj.data.DataSource;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.ListBackedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101.Record;

public class ClustererBuilder {
	private static final Map<String, DatasetBuilder<ListDataset<MBFImage>>>
		DATASETS = new HashMap<String, DatasetBuilder<ListDataset<MBFImage>>>();
	
	private static void init() {
		DATASETS.put("Caltech101", new Caltech101DatasetBuilder());
	}
	
	/**
	 * A tool for automatically training clusterers.
	 * 
	 * @param args
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws BuildException 
	 */
	public static void main(String[] args) throws ParseException, IOException, BuildException {
		init();
		
		Options options = new Options();
		options.addOption("C", true, "type of clusterer");
		options.addOption("T", true, "training set to use");
		options.addOption("V", true, "validation set to use");
		options.addOption("o", true, "output file for clusterer information");
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		
		String clustererType = cmd.getOptionValue("C");
		String trainingSet = cmd.getOptionValue("T");
		String validationSet = cmd.getOptionValue("V");
		String outFile = cmd.getOptionValue("o");
		
		Dataset trainingSource;
		Dataset validationSource;
		File out;
		
		try {
			if (clustererType == null) {
				throw new RequiredArgumentException("C");
			}
			
			if (trainingSet == null) {
				throw new RequiredArgumentException("T");
			}
			trainingSource = establishDataset(trainingSet);
			
			if (validationSet == null) {
				throw new RequiredArgumentException("V");
			}
			validationSource = establishDataset(validationSet);
			
			if (outFile == null) {
				throw new RequiredArgumentException("o");
			}
			out = Paths.get(outFile).toFile();
			
		} catch (UserInputException e) {
			System.err.println(e.getMessage());
			return;
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
	 * @throws BuildException 
	 */
	private static Dataset<MBFImage> establishDataset(String sourceString) throws IOException, InvalidDatasetException, BuildException {
		// Datasets are comma-separated.
		String[] subSets = sourceString.split(";");
		
		AggregateDataset<MBFImage> dataset = new AggregateDataset<MBFImage>();
		
		for (String subSet : subSets) {
			String[] components = subSet.split(":", 2);
			
			DatasetBuilder<ListDataset<MBFImage>> builder = DATASETS.get(components[0]);
			
			if (builder == null) {
				throw new InvalidDatasetException(components[0]);
			}
			
			String[] args = null;
			if (components.length == 2) {
				args = components[1].split(",");
			}
			
			dataset.add(builder.build(args));
		}
		
		return dataset;
	}

}
