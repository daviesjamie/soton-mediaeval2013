package uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Arrays;

import org.openimaj.util.iterator.TextLineIterable;

public class CEDDExtractor {
	public static void main(String[] args) throws Exception {
		final File[] files = new File("/Volumes/My Book/mediaeval-placing/").listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("imagefeatures") && name.endsWith(".gz");
			}
		});

		System.out.println(Arrays.toString(files));

		final DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(
				"/Volumes/SSD/mediaeval13/placing/cedd.bin"))));

		int count = 0;
		for (final File f : files) {
			System.out.println("Processing " + f);

			for (final String line : new TextLineIterable(new TextLineIterable.GZIPFileProvider(f))) {
				final String[] parts = line.split(" ");

				try {
					final long flickrId = Long.parseLong(parts[0]);
					final byte[] cedd = new byte[144];

					try {
						for (int i = 1; i < parts.length; i++) {
							if (parts[i - 1].equals("cedd")) {

								for (int j = 0; j < 144; j++)
									cedd[j] = Byte.parseByte(parts[i + j + 1]);

								break;
							}
						}
					} catch (final NumberFormatException nfe) {
						Arrays.fill(cedd, (byte) 0);
					}

					dos.writeLong(flickrId);
					dos.write(cedd);
				} catch (final NumberFormatException nfe2) {

				}
				if (count++ % 1000 == 0)
					System.out.println(count);
			}
		}

		dos.close();
	}
}
