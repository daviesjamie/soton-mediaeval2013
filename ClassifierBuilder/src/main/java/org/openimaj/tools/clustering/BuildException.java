package org.openimaj.tools.clustering;

import java.io.IOException;

public class BuildException extends Exception {
	
	public BuildException(String message) {
		super("Error building dataset: \n" + message);
	}

	public BuildException(Exception cause) {
		super(cause);
	}
}
