package org.openimaj.util.data.dataset;

public class BuildException extends Exception {
	
	private static final long serialVersionUID = -8746205951315944656L;

	public BuildException(String message) {
		super("Error building: \n" + message);
	}

	public BuildException(Exception cause) {
		super(cause);
	}
}
