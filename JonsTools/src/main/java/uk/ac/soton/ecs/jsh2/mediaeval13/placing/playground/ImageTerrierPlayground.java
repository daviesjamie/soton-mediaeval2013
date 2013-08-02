package uk.ac.soton.ecs.jsh2.mediaeval13.placing.playground;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.ImageTerrierSearcher;

public class ImageTerrierPlayground {
	public static void main(String[] args) throws IOException {
		final String index = "/Volumes/SSD/mediaeval13/placing/sift1x-quant1m-basic.idx";
		final String vocab = "/Volumes/SSD/mediaeval13/codebooks/mirflickr-1000000-sift-fastkmeans-new.idx";

		final String luceneIndex =
				"/Volumes/SSD/mediaeval13/placing/places.lucene";

		final Directory directory = new SimpleFSDirectory(new File(luceneIndex));
		final IndexSearcher luceneSearcher = new
				IndexSearcher(DirectoryReader.open(directory));
		final ImageTerrierSearcher visualSearcher = new ImageTerrierSearcher(new File(index),
				new File(vocab), luceneSearcher);

		final int numResults = 20;

		System.out.println("Ready for query: ");
		String line;
		final BufferedReader br = new BufferedReader(new
				InputStreamReader(System.in));
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.length() == 0)
				continue;

			ScoreDoc[] results = null;
			try {
				final long query = Long.parseLong(line);
				results = visualSearcher.search(query, numResults);
			} catch (final NumberFormatException e) {
				try {
					final URL u = new URL(line);
					results = visualSearcher.search(ImageUtilities.readMBF(u), numResults);
				} catch (final MalformedURLException ee) {
					continue;
				}
			}

			final List<MBFImage> images = new ArrayList<MBFImage>();
			final FImage img = new FImage(360, 180);
			for (int i = 0; i < results.length; i++) {
				final ScoreDoc r = results[i];
				final Document d = luceneSearcher.doc(r.doc);
				final String url = d.get(LuceneIndexBuilder.FIELD_URL);

				final String[] llstr = d.get("location").split(" ");
				final float x = Float.parseFloat(llstr[0]) + 180;
				final float y = 90 - Float.parseFloat(llstr[1]);
				img.pixels[(int) (y % img.height)][(int) (x % img.width)]++;

				System.out.println(url + "\t" + r.score);
				images.add(ImageUtilities.readMBF(new URL(url)));
			}
			DisplayUtilities.display("result", images);
			DisplayUtilities.display(img.normalise(), "map");

			System.out.println();
			System.out.println("Ready for query: ");
		}
	}

}
