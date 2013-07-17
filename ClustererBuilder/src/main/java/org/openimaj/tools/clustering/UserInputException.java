package org.openimaj.tools.clustering;

public class UserInputException extends Exception {

	public UserInputException(String message) {
		super("Erroneous user input: \n" + message);
	}

}
