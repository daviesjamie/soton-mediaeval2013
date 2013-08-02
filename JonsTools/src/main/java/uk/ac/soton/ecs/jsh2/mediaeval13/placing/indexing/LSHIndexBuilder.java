package uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing;

import gnu.trove.list.array.TLongArrayList;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.Text;

public class LSHIndexBuilder {
	public static void main(String[] args) throws IOException {
		final Configuration conf = new Configuration();
		final File base = new File("/Volumes/My Book/testIndex/");
		base.mkdirs();

		final String csvPath = "/Volumes/SSD/mediaeval13/placing/all.csv";
		final TLongArrayList ids = getIds(csvPath);
		writeIds(base, ids);

		for (int i = 0; i < 4; i++) {
			final Path path = new Path("hdfs://seurat/data/mediaeval/placing/sift1x-lsh.seq/part-r-0000" + i);
			final Reader reader = new Reader(path.getFileSystem(conf), path, conf);

			final DataOutputStream idx = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
					new File(base, "lsh-index." + i))));
			final DataOutputStream data = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
					new File(base, "lsh-index-data." + i))));

			final IntWritable key = new IntWritable();
			final Text value = new Text();

			long offset = 0;
			long recCount = 0;
			while (reader.next(key, value)) {
				idx.writeInt(key.get());
				idx.writeLong(offset);

				final String[] parts = value.toString().split(" ");
				for (final String s : parts) {
					final int id = ids.binarySearch(Long.parseLong(s));
					data.writeInt(id);
				}
				offset += parts.length * 4;

				if (recCount % 1000000 == 0)
					System.out.println(i + " " + recCount);

				recCount++;
			}
			reader.close();
			idx.close();
			data.close();
		}
	}

	private static void writeIds(final File base, TLongArrayList ids) throws FileNotFoundException, IOException {
		final DataOutputStream idx = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
				new File(base, "ids.meta"))));

		for (int i = 0; i < ids.size(); i++) {
			idx.writeLong(ids.get(i));
		}

		idx.close();
	}

	private static TLongArrayList getIds(final String csvPath) throws FileNotFoundException, IOException {
		final TLongArrayList ids = new TLongArrayList(7600000);
		final BufferedReader br = new BufferedReader(new FileReader(csvPath));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("photo"))
				continue;

			final String[] parts = line.split(" ");

			ids.add(Long.parseLong(parts[0]));
		}
		br.close();

		ids.sort();

		return ids;
	}
}
