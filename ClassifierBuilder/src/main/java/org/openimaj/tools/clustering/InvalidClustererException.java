package org.openimaj.tools.clustering;

public class InvalidClustererException extends UserInputException {

	public InvalidClustererException(String message) {
		super("Invalid clusterer: \n" + message);
	}

}
