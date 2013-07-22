package org.openimaj.tools.clustering;

public class RequiredArgumentException extends UserInputException {
	public RequiredArgumentException(String argument) {
		super("Required argument: \n" + argument);
	}
}
