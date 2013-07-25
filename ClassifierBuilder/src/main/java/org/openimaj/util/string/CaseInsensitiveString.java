package org.openimaj.util.string;

/**
 * Case-insensitive wrapper for Strings to allow case-insensitive comparison in 
 * Collection<String>'s.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class CaseInsensitiveString {
	private String string;
	
	public CaseInsensitiveString(String string) {
		this.string = string.toLowerCase();
	}
	
	public boolean equals(Object other) {
		if (other instanceof String) {
			return ((String) other).compareToIgnoreCase(string) == 0;
		} else if (other instanceof CaseInsensitiveString) {
			return ((CaseInsensitiveString) other).string.compareToIgnoreCase(string) == 0;
		} else {
			return false;
		}
	}
	
	public int hashCode() {
		return string.hashCode();
	}
	
	public String toString() {
		return string;
	}
}
