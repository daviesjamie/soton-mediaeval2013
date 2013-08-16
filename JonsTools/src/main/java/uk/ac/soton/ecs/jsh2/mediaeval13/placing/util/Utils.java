package uk.ac.soton.ecs.jsh2.mediaeval13.placing.util;

import gnu.trove.list.array.TLongArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;

public class Utils {
	private Utils() {
	}

	/**
	 * Read the lat-lng file into a list of {@link GeoLocation}s
	 * 
	 * @param latlngFile
	 * @param skipIds
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static List<GeoLocation> readLatLng(File latlngFile, TLongArrayList skipIds) throws FileNotFoundException,
			IOException
	{
		final ArrayList<GeoLocation> pts = new ArrayList<GeoLocation>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(latlngFile));

			String line;
			while ((line = br.readLine()) != null) {
				try {
					final String[] parts = line.split(" ");
					final long id = Long.parseLong(parts[0]);
					final double lat = Double.parseDouble(parts[1]);
					final double lng = Double.parseDouble(parts[2]);

					if (skipIds.binarySearch(id) < 0)
						pts.add(new GeoLocation(lat, lng));
				} catch (final NumberFormatException nfe) {
					// ignore line
				}
			}
		} finally {
			if (br != null)
				br.close();
		}
		return pts;
	}
}
