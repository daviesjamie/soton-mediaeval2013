package org.openimaj.mediaeval.searchhyper2013.util;

public abstract class CSV {

	public static String arrayToCSV(Object[] array) {
		StringBuilder stringBuilder = new StringBuilder();
		
		for (Object obj : array) {
			stringBuilder.append(obj.toString() + ", ");
		}
		
		stringBuilder.replace(stringBuilder.length() - 2,
							  stringBuilder.length(),
							  "");
		
		return stringBuilder.toString();
	}
}
