package org.openimaj.tools;

public class UserInputException extends Exception {

	private static final long serialVersionUID = 2656483645125677802L;

	public UserInputException(String message) {
		super("Erroneous user input: \n" + message);
	}

}
