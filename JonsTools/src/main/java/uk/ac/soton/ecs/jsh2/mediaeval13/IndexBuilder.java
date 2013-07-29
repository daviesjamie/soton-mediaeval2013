package uk.ac.soton.ecs.jsh2.mediaeval13;

import gnu.trove.set.hash.TLongHashSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.openimaj.hadoop.sequencefile.SequenceFileUtility;
import org.openimaj.hadoop.sequencefile.TextBytesSequenceFileUtility;
import org.openimaj.image.indexing.vlad.VLADIndexerData;
import org.openimaj.io.IOUtils;

/**
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class IndexBuilder {
	public static void main(String[] args) throws IOException {
		final String dataPath = "hdfs://seurat/data/mediaeval/placing/sift1x-vlad/";
		final String latlngPath = "/Volumes/SSD/training_latlng";
		final String vladPath = "/Volumes/SSD/sift1x-vlad64n-pca128-pq16.bin";
		final String outputPath = "/Volumes/SSD/mediaeval13/placing/sift1x-vlad64n-pca128-pq16-adcnn.idx";

		final long[] trainingIds = loadTrainingIds(latlngPath);
		final byte[][] pqdata = extractPQ(trainingIds, dataPath);

		System.out.println("Reading vlad indexer data");
		final VLADIndexerData vladData = IOUtils.readFromFile(new File(vladPath));

		System.out.println("Building and saving search engine");
		final VLADSearchEngine searcher = new VLADSearchEngine(trainingIds, vladData, pqdata);

		final File out = new File(outputPath);
		out.getParentFile().mkdirs();
		IOUtils.writeToFile(searcher, out);
	}

	private static byte[][] extractPQ(final long[] trainingIds, final String dataPath) throws IOException {
		System.out.println("Extracting PQ-PCA-VLAD vectors");

		final Path[] pqpcavlads = SequenceFileUtility.getFilePaths(dataPath, "part-m-");

		final byte[][] pqdata = new byte[trainingIds.length][];
		for (final Path p : pqpcavlads) {
			final TextBytesSequenceFileUtility sf = new TextBytesSequenceFileUtility(p.toString(), true);

			for (final Entry<Text, BytesWritable> entry : sf) {
				final long id = Long.parseLong(entry.getKey().toString());
				final byte[] data = Arrays.copyOf(entry.getValue().getBytes(), entry.getValue().getLength());

				final int idx = Arrays.binarySearch(trainingIds, id);

				if (idx < 0) {
					continue;
				}

				if (pqdata[idx] != null)
					System.err.println("Seemingly duplicate image " + id);
				pqdata[idx] = data;
			}
		}

		for (int i = 0; i < pqdata.length; i++) {
			if (pqdata[i] == null) {
				pqdata[i] = new byte[pqdata[0].length];
				System.out.println("Adding empty feature for " + trainingIds[i]);
			}
		}

		return pqdata;
	}

	private static long[] loadTrainingIds(String string) throws IOException {
		System.out.println("Reading IDs for geolocated images");
		final TLongHashSet ids = new TLongHashSet(7600000);

		final BufferedReader br = new BufferedReader(new FileReader(string));
		String line;
		br.readLine(); // skip first line
		while ((line = br.readLine()) != null) {
			final String[] parts = line.split(" ");

			ids.add(Long.parseLong(parts[0]));
		}
		br.close();

		final long[] result = ids.toArray();

		System.out.println("Sorting IDs");
		Arrays.sort(result);

		return result;
	}
}
