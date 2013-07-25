package org.openimaj.tools;


public class RequiredArgumentException extends UserInputException {

	private static final long serialVersionUID = -2872708140462444819L;

	public RequiredArgumentException(String argument) {
		super("Required argument: \n" + argument);
	}
}
