package org.openimaj.tools.classification.classifierbuilder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.commons.cli.ParseException;
import org.openimaj.tools.features.featureextractor.FeatureExtractorBuilder;
import org.openimaj.util.data.dataset.BuildException;

public abstract class Main {

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, NoSuchAlgorithmException, ParseException, IOException, BuildException {
		if (args.length == 0) {
			printHelp("Missing argument");
			return;
		}
		
		String[] otherArgs = Arrays.copyOfRange(args, 1, args.length);
		
		switch (args[0]) {
		case "ClassifierBuilder":		ClassifierBuilder.main(otherArgs);			break;
		case "FeatureExtractorBuilder":	FeatureExtractorBuilder.main(otherArgs);	break;
		default:		printHelp("Unsupported mode");
		}
	}

	private static void printHelp(String string) {
		System.err.println(string);
		
		String help = "\n" +
"The first argument should specify which builder to run. Valid options are:\n" +
"\n" +
"ClassifierBuilder\n" +
"FeatureExtractorBuilder\n" +
"\n" +
"They specify their own interface.";
		
		System.out.println(help);
	}

}
