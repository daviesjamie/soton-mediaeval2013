package uk.ac.soton.ecs.jsh2.mediaeval13.diversity;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class GetGeos {
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		final File baseDir = new File("/Users/jon/Data/mediaeval/diversity/");

		for (final ResultList rl : ResultList.loadResultSet(baseDir, true)) {
			System.out.println(rl.monument);
			for (final ResultItem ri : rl) {
				final double lat = ri.latitude;
				final double lon = ri.longitude;

				if (lat != 0 && lon != 0) {
					System.out.println(lon + "\t" + lat);
				}
			}
		}
	}
}
