package uk.ac.soton.ecs.jsh2.mediaeval13.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FileUtils {
	/**
	 * Reads a single line from a file based on the line starting with the given
	 * key followed directly by a comma
	 * 
	 * @param f
	 *            the file
	 * @param key
	 *            the keu
	 * @return the line (with the key portion removed)
	 * @throws IOException
	 */
	public static String readKeyedLine(File f, String key) throws IOException {
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(f));

			String line;
			while ((line = br.readLine()) != null)
				if (line.startsWith(key + ","))
					return line.substring(key.length() + 1);
		} finally {
			if (br != null)
				br.close();
		}
		return null;
	}

	/**
	 * Reads a single line from a file based on the line starting with the given
	 * key followed directly by the given separator
	 * 
	 * @param f
	 *            the file
	 * @param key
	 *            the key
	 * @return the line (with the key portion removed)
	 * @throws IOException
	 */
	public static String readKeyedLine(File f, String key, String sep) throws IOException {
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(f));

			String line;
			while ((line = br.readLine()) != null)
				if (line.startsWith(key + sep))
					return line.substring(key.length() + 1);
		} finally {
			if (br != null)
				br.close();
		}
		return null;
	}

}
