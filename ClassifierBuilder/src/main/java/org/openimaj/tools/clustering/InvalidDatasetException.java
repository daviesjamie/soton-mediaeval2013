package org.openimaj.tools.clustering;

public class InvalidDatasetException extends UserInputException {

	public InvalidDatasetException(String dataset) {
		super("Invalid dataset: \n" + dataset);
	}

}
