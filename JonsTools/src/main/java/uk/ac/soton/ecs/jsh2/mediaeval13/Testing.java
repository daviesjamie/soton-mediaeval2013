package uk.ac.soton.ecs.jsh2.mediaeval13;

import gnu.trove.list.array.TLongArrayList;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;

import org.openimaj.image.ImageUtilities;
import org.openimaj.io.IOUtils;

import cern.colt.Arrays;

public class Testing {
	public static void main(String[] args) throws IOException {
		System.out.println("reading searchengine");
		final VLADSearchEngine se = IOUtils.readFromFile(new File(
				"/Volumes/SSD/mediaeval13/placing/sift1x-vlad64n-pca128-pq16-adcnn.idx"));

		final File file = new File("/Volumes/SSD/mediaeval13/placing/sift1x-vlad64n-pca128.dat");

		System.out.println("reading features");
		final TLongArrayList allIds = new TLongArrayList(7800000);
		final DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		try {
			while (true) {
				allIds.add(dis.readLong());

				int rem = 128 * 4;
				while (rem != 0) {
					final long r = dis.skip(rem);
					if (r > 0)
						rem -= r;
				}
			}
		} catch (final IOException e) {
			dis.close();
		}

		final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		final RandomAccessFile raf = new RandomAccessFile(file, "r");

		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.length() == 0)
				continue;

			final long query;

			try {
				query = Long.parseLong(line);

				final long idx = allIds.binarySearch(query);
				if (idx < 0) {
					System.out.println("image not found");
					continue;
				}

				raf.seek(idx * (8 + 4 * 128));
				if (raf.readLong() != query) {
					throw new IOException();
				}
				final float[] vec = new float[128];
				for (int i = 0; i < 128; i++)
					vec[i] = raf.readFloat();

				System.out.println(Arrays.toString(vec));
				System.out.println(se.search(vec, 10));
			} catch (final NumberFormatException e) {
				try {
					final URL u = new URL(line);
					final float[] vec = se.data.extractPcaVlad(ImageUtilities.readMBF(u));
					System.out.println(Arrays.toString(vec));
					System.out.println(se.search(vec, 10));
				} catch (final MalformedURLException ee) {
					continue;
				}
			}

		}
	}
}
