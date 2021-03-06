package uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.openimaj.hadoop.sequencefile.SequenceFileUtility;
import org.openimaj.hadoop.sequencefile.TextBytesSequenceFileUtility;
import org.openimaj.util.pair.LongObjectPair;

/**
 * This class extracts the contents of the pcavlad-m-xxxxx files from the hdfs
 * and writes them to a local binary file that contains packed pairs of flickr
 * ids (long) and pca-vlad features (float[]).
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class PcaVladFeatureBuilder {
	public static void main(String[] args) throws IOException {
		final String dataPath =
				"hdfs://seurat/data/mediaeval/placing/rgb-sift1x-vlad.seq/";
		final String outputPath =
				"/Volumes/SSD/mediaeval13/placing/vlad-indexes/rgb-sift1x-vlad64n-pca128.dat";
		// final String dataPath =
		// "hdfs://seurat/data/mediaeval/placing/sift1x-vlad.seq/";
		// final String outputPath =
		// "/Volumes/SSD/mediaeval13/placing/vlad-indexes/sift1x-vlad64n-pca128.dat";

		extract(dataPath, outputPath);
	}

	private static void extract(final String dataPath, final String outputPath) throws IOException,
			FileNotFoundException
	{
		final Path[] pcavlads = SequenceFileUtility.getFilePaths(dataPath, "pcavlad-m-");

		final List<LongObjectPair<float[]>> data = new ArrayList<LongObjectPair<float[]>>(7900000);
		for (final Path p : pcavlads) {
			System.out.println(p);
			final TextBytesSequenceFileUtility sf = new TextBytesSequenceFileUtility(p.toString(), true);

			for (final Entry<Text, BytesWritable> entry : sf) {
				final long id = Long.parseLong(entry.getKey().toString());

				final float[] fdata = new float[entry.getValue().getLength() / 4];
				final DataInputStream dos = new DataInputStream(new ByteArrayInputStream(entry.getValue().getBytes()));
				for (int i = 0; i < fdata.length; i++)
					fdata[i] = dos.readFloat();

				data.add(LongObjectPair.pair(id, fdata));
			}
		}

		Collections.sort(data, new Comparator<LongObjectPair<float[]>>() {
			@Override
			public int compare(LongObjectPair<float[]> o1, LongObjectPair<float[]> o2) {
				if (o1.first < o2.first)
					return -1;
				if (o1.first > o2.first)
					return 1;
				return 0;
			}
		});

		final DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputPath)));
		for (final LongObjectPair<float[]> item : data) {
			dos.writeLong(item.first);
			for (int i = 0; i < item.second.length; i++)
				dos.writeFloat(item.second[i]);
		}
		dos.close();
	}
}
