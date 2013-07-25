package org.openimaj.util.data.dataset;

import org.openimaj.tools.UserInputException;

public class InvalidDatasetException extends UserInputException {

	private static final long serialVersionUID = -2190583655912807006L;

	public InvalidDatasetException(String dataset) {
		super("Invalid dataset: \n" + dataset);
	}

}
