package org.openimaj.mediaeval.searchhyper2013.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Files {

	public static List<File> removeExcluded(File dir, List<String> excludes) {
		List<File> files = new ArrayList<File>();
		for (File file : dir.listFiles()) {
			if (!excludes.contains(file.getName().split("\\.")[0])) {
				files.add(file);
			}
		}
		
		return files;
	}
}
