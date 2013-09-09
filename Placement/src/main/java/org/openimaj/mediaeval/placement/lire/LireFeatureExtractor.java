package org.openimaj.mediaeval.placement.lire;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Arrays;

import net.semanticmetadata.lire.imageanalysis.AutoColorCorrelogram;
import net.semanticmetadata.lire.imageanalysis.BasicFeatures;
import net.semanticmetadata.lire.imageanalysis.CEDD;
import net.semanticmetadata.lire.imageanalysis.ColorLayout;
import net.semanticmetadata.lire.imageanalysis.EdgeHistogram;
import net.semanticmetadata.lire.imageanalysis.FCTH;
import net.semanticmetadata.lire.imageanalysis.Gabor;
import net.semanticmetadata.lire.imageanalysis.ScalableColor;
import net.semanticmetadata.lire.imageanalysis.SimpleColorHistogram;
import net.semanticmetadata.lire.imageanalysis.Tamura;
import net.semanticmetadata.lire.imageanalysis.joint.JointHistogram;
import net.semanticmetadata.lire.imageanalysis.joint.RankAndOpponent;

import org.openimaj.util.iterator.TextLineIterable;

/**
 * A quick and dirty script to extract LIRE features from the imagefeatures
 * compressed files into separate binary files for each feature, in the format
 * <code><flickrId> <feature></code>
 * 
 * @author Jamie Davies (jagd1g11@ecs.soton.ac.uk)
 */
public class LireFeatureExtractor {

	/** The directory to output the binary files to **/
	private static final String OUTPUT_DIR = "data/features/";

	/** The input files (imagesfeatures*.gz) **/
	protected static final File[] INPUT_FILES = new File("data/").listFiles(new FilenameFilter() {

		@Override
		public boolean accept(File dir, String name) {
			return name.startsWith("imagefeatures") && name.endsWith(".gz");
		}
	});

	public static void main(String args[]) throws Exception {
		/** Lengths of the features **/
		final int acc_length = 1024;
		final int bf_length = 8;
		final int cedd_length = 144;
		final int col_length = 118;
		final int edgehistogram_length = 80;
		final int fcth_length = 192;
		final int ophist_length = 576;
		final int gabor_length = 60;
		final int jhist_length = 576;
		final int jophist_length = 576;
		final int scalablecolor_length = 64;
		final int rgb_length = 512;
		final int tamura_length = 18;

		/** The DataOutputStreams for the separate features files **/
		final DataOutputStream acc_dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(
				OUTPUT_DIR + "acc.bin"))));
		final DataOutputStream bf_dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(
				OUTPUT_DIR + "bf.bin"))));
		final DataOutputStream cedd_dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(
				OUTPUT_DIR + "cedd.bin"))));
		final DataOutputStream col_dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(
				OUTPUT_DIR + "col.bin"))));
		final DataOutputStream edgehistogram_dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
				new File(OUTPUT_DIR + "edgehistogram.bin"))));
		final DataOutputStream fcth_dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(
				OUTPUT_DIR + "fcth.bin"))));
		final DataOutputStream ophist_dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
				new File(OUTPUT_DIR + "ophist.bin"))));
		final DataOutputStream gabor_dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(
				OUTPUT_DIR + "gabor.bin"))));
		final DataOutputStream jhist_dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(
				OUTPUT_DIR + "jhist.bin"))));
		final DataOutputStream jophist_dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
				new File(OUTPUT_DIR + "jophist.bin"))));
		final DataOutputStream scalablecolor_dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
				new File(OUTPUT_DIR + "scalablecolor.bin"))));
		final DataOutputStream rgb_dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(
				OUTPUT_DIR + "rgb.bin"))));
		final DataOutputStream tamura_dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
				new File(OUTPUT_DIR + "tamura.bin"))));

		/**
		 * The LireFeatureCalculators used to calculate the features if they
		 * cannot be read
		 **/
		final LireFeatureCalculator acc_calc = new LireFeatureCalculator(new AutoColorCorrelogram());
		final LireFeatureCalculator bf_calc = new LireFeatureCalculator(new BasicFeatures());
		final LireFeatureCalculator cedd_calc = new LireFeatureCalculator(new CEDD());
		final LireFeatureCalculator col_calc = new LireFeatureCalculator(new ColorLayout());
		final LireFeatureCalculator edgehistogram_calc = new LireFeatureCalculator(new EdgeHistogram());
		final LireFeatureCalculator fcth_calc = new LireFeatureCalculator(new FCTH());
		final LireFeatureCalculator ophist_calc = new LireFeatureCalculator(new RankAndOpponent());
		final LireFeatureCalculator gabor_calc = new LireFeatureCalculator(new Gabor());
		final LireFeatureCalculator jhist_calc = new LireFeatureCalculator(new JointHistogram());
		final LireFeatureCalculator jophist_calc = new LireFeatureCalculator(new RankAndOpponent());
		final LireFeatureCalculator scalablecolor_calc = new LireFeatureCalculator(new ScalableColor());
		final LireFeatureCalculator rgb_calc = new LireFeatureCalculator(new SimpleColorHistogram());
		final LireFeatureCalculator tamura_calc = new LireFeatureCalculator(new Tamura());

		/** The offsets (in fields) of each feature in the file **/
		int acc_offset = 0;
		int bf_offset = 0;
		int cedd_offset = 0;
		int col_offset = 0;
		int edgehistogram_offset = 0;
		int fcth_offset = 0;
		int ophist_offset = 0;
		int gabor_offset = 0;
		int jhist_offset = 0;
		int jophist_offset = 0;
		int scalablecolor_offset = 0;
		int rgb_offset = 0;
		int tamura_offset = 0;

		/** The counts of failed imports for each feature **/
		int id_fails = 0;
		int acc_fails = 0;
		int bf_fails = 0;
		int cedd_fails = 0;
		int col_fails = 0;
		int edgehistogram_fails = 0;
		int fcth_fails = 0;
		int ophist_fails = 0;
		int gabor_fails = 0;
		int jhist_fails = 0;
		int jophist_fails = 0;
		int scalablecolor_fails = 0;
		int rgb_fails = 0;
		int tamura_fails = 0;

		/** The number of processed images so far **/
		int processed = 0;

		System.out.println("Calculating the offsets...");
		final String firstLine = new TextLineIterable(new TextLineIterable.GZIPFileProvider(INPUT_FILES[0])).iterator()
				.next();
		final String[] firstParts = firstLine.split(" ");
		for (int i = 0; i < firstParts.length; i++) {
			switch (firstParts[i]) {
			case "acc":
				acc_offset = i;
				break;
			case "bf":
				bf_offset = i;
				break;
			case "cedd":
				cedd_offset = i;
				break;
			case "col":
				col_offset = i;
				break;
			case "edgehistogram":
				edgehistogram_offset = i;
				break;
			case "fcth":
				fcth_offset = i;
				break;
			case "ophist":
				ophist_offset = i;
				break;
			case "gabor":
				gabor_offset = i;
				break;
			case "jhist":
				jhist_offset = i;
				break;
			case "jophist":
				jophist_offset = i;
				break;
			case "scalablecolor":
				scalablecolor_offset = i;
				break;
			case "RGB":
				rgb_offset = i;
				break;
			case "tamura":
				tamura_offset = i;
				break;
			}
		}

		for (final File f : INPUT_FILES) {
			System.out.println("Processing " + f + "...");

			for (final String line : new TextLineIterable(new TextLineIterable.GZIPFileProvider(f))) {
				final String[] parts = line.split(" ");

				try {
					final long flickrId = Long.parseLong(parts[0]);

					float[] acc = new float[acc_length];
					float[] bf = new float[bf_length];
					byte[] cedd = new byte[cedd_length];
					byte[] col = new byte[col_length];
					byte[] edgehistogram = new byte[edgehistogram_length];
					byte[] fcth = new byte[fcth_length];
					byte[] ophist = new byte[ophist_length];
					float[] gabor = new float[gabor_length];
					byte[] jhist = new byte[jhist_length];
					byte[] jophist = new byte[jophist_length];
					byte[] scalablecolor = new byte[scalablecolor_length];
					byte[] rgb = new byte[rgb_length];
					float[] tamura = new float[tamura_length];

					try {
						for (int i = acc_offset; i < acc_length; i++)
							acc[i] = Float.parseFloat(parts[acc_offset + i]);
					} catch (Exception e) {
						try {
							acc = acc_calc.extractFloatFeature(flickrId);
						} catch (Exception e1) {
							acc_fails++;
							Arrays.fill(acc, 0f);
						}
					}

					try {
						for (int i = bf_offset; i < bf_length; i++)
							bf[i] = Float.parseFloat(parts[bf_offset + i]);
					} catch (Exception e) {
						try {
							bf = bf_calc.extractFloatFeature(flickrId);
						} catch (Exception e1) {
							bf_fails++;
							Arrays.fill(bf, 0f);
						}
					}

					try {
						for (int i = cedd_offset; i < cedd_length; i++)
							cedd[i] = Byte.parseByte(parts[cedd_offset + i]);
					} catch (Exception e) {
						try {
							cedd = cedd_calc.extractByteFeature(flickrId);
						} catch (Exception e1) {
							cedd_fails++;
							Arrays.fill(cedd, (byte) 0);
						}
					}

					try {
						for (int i = col_offset; i < col_length; i++)
							col[i] = Byte.parseByte(parts[col_offset + i]);
					} catch (Exception e) {
						try {
							col = col_calc.extractByteFeature(flickrId);
						} catch (Exception e1) {
							col_fails++;
							Arrays.fill(col, (byte) 0);
						}
					}

					try {
						for (int i = edgehistogram_offset; i < edgehistogram_length; i++)
							edgehistogram[i] = Byte.parseByte(parts[edgehistogram_offset + i]);
					} catch (Exception e) {
						try {
							edgehistogram = edgehistogram_calc.extractByteFeature(flickrId);
						} catch (Exception e1) {
							edgehistogram_fails++;
							Arrays.fill(edgehistogram, (byte) 0);
						}
					}

					try {
						for (int i = fcth_offset; i < fcth_length; i++)
							fcth[i] = Byte.parseByte(parts[fcth_offset + i]);
					} catch (Exception e) {
						try {
							fcth = fcth_calc.extractByteFeature(flickrId);
						} catch (Exception e1) {
							fcth_fails++;
							Arrays.fill(fcth, (byte) 0);
						}
					}

					try {
						for (int i = ophist_offset; i < ophist_length; i++)
							ophist[i] = Byte.parseByte(parts[ophist_offset + i]);
					} catch (Exception e) {
						try {
							ophist = ophist_calc.extractByteFeature(flickrId);
						} catch (Exception e1) {
							ophist_fails++;
							Arrays.fill(ophist, (byte) 0);
						}
					}

					try {
						for (int i = gabor_offset; i < gabor_length; i++)
							gabor[i] = Float.parseFloat(parts[gabor_offset + i]);
					} catch (Exception e) {
						try {
							gabor = gabor_calc.extractFloatFeature(flickrId);
						} catch (Exception e1) {
							gabor_fails++;
							Arrays.fill(gabor, 0f);
						}
					}

					try {
						for (int i = jhist_offset; i < jhist_length; i++)
							jhist[i] = Byte.parseByte(parts[jhist_offset + i]);
					} catch (Exception e) {
						try {
							jhist = jhist_calc.extractByteFeature(flickrId);
						} catch (Exception e1) {
							jhist_fails++;
							Arrays.fill(jhist, (byte) 0);
						}
					}

					try {
						for (int i = jophist_offset; i < jophist_length; i++)
							jophist[i] = Byte.parseByte(parts[jophist_offset + i]);
					} catch (Exception e) {
						try {
							jophist = jophist_calc.extractByteFeature(flickrId);
						} catch (Exception e1) {
							jophist_fails++;
							Arrays.fill(jophist, (byte) 0);
						}
					}

					try {
						for (int i = scalablecolor_offset; i < scalablecolor_length; i++)
							scalablecolor[i] = Byte.parseByte(parts[scalablecolor_offset + i]);
					} catch (Exception e) {
						try {
							scalablecolor = scalablecolor_calc.extractByteFeature(flickrId);
						} catch (Exception e1) {
							scalablecolor_fails++;
							Arrays.fill(scalablecolor, (byte) 0);
						}
					}

					try {
						for (int i = rgb_offset; i < rgb_length; i++)
							rgb[i] = Byte.parseByte(parts[rgb_offset + i]);
					} catch (Exception e) {
						try {
							rgb = rgb_calc.extractByteFeature(flickrId);
						} catch (Exception e1) {
							rgb_fails++;
							Arrays.fill(rgb, (byte) 0);
						}
					}

					try {
						for (int i = tamura_offset; i < tamura_length; i++)
							tamura[i] = Float.parseFloat(parts[tamura_offset + i]);
					} catch (Exception e) {
						try {
							tamura = tamura_calc.extractFloatFeature(flickrId);
						} catch (Exception e1) {
							tamura_fails++;
							Arrays.fill(tamura, 0f);
						}
					}

					acc_dos.writeLong(flickrId);
					for (int i = 0; i < acc.length; i++)
						acc_dos.writeFloat(acc[i]);

					bf_dos.writeLong(flickrId);
					for (int i = 0; i < bf.length; i++)
						bf_dos.writeFloat(bf[i]);

					cedd_dos.writeLong(flickrId);
					cedd_dos.write(cedd);

					col_dos.writeLong(flickrId);
					col_dos.write(col);

					edgehistogram_dos.writeLong(flickrId);
					edgehistogram_dos.write(edgehistogram);

					fcth_dos.writeLong(flickrId);
					fcth_dos.write(fcth);

					ophist_dos.writeLong(flickrId);
					ophist_dos.write(ophist);

					gabor_dos.writeLong(flickrId);
					for (int i = 0; i < gabor.length; i++)
						gabor_dos.writeFloat(gabor[i]);

					jhist_dos.writeLong(flickrId);
					jhist_dos.write(jhist);

					jophist_dos.writeLong(flickrId);
					jophist_dos.write(jophist);

					scalablecolor_dos.writeLong(flickrId);
					scalablecolor_dos.write(scalablecolor);

					rgb_dos.writeLong(flickrId);
					rgb_dos.write(rgb);

					tamura_dos.writeLong(flickrId);
					for (int i = 0; i < tamura.length; i++)
						tamura_dos.writeFloat(tamura[i]);
				} catch (final NumberFormatException nfe) {
					id_fails++;
				}

				if (++processed % 1000 == 0)
					System.out.println(processed);
			}
		}

		acc_dos.close();
		bf_dos.close();
		cedd_dos.close();
		col_dos.close();
		edgehistogram_dos.close();
		fcth_dos.close();
		ophist_dos.close();
		gabor_dos.close();
		jhist_dos.close();
		jophist_dos.close();
		scalablecolor_dos.close();
		rgb_dos.close();
		tamura_dos.close();

		System.out.println("Done.");

		System.out.println("Failed to get id for " + id_fails + " photos.");
		System.out.println("Failed to get acc for " + acc_fails + " photos.");
		System.out.println("Failed to get bf for " + bf_fails + " photos.");
		System.out.println("Failed to get cedd for " + cedd_fails + " photos.");
		System.out.println("Failed to get col for " + col_fails + " photos.");
		System.out.println("Failed to get edgehistogram for " + edgehistogram_fails + " photos.");
		System.out.println("Failed to get fcth for " + fcth_fails + " photos.");
		System.out.println("Failed to get ophist for " + ophist_fails + " photos.");
		System.out.println("Failed to get gabor for " + gabor_fails + " photos.");
		System.out.println("Failed to get jhist for " + jhist_fails + " photos.");
		System.out.println("Failed to get jophist for " + jophist_fails + " photos.");
		System.out.println("Failed to get scalablecolor for " + scalablecolor_fails + " photos.");
		System.out.println("Failed to get rgb for " + rgb_fails + " photos.");
		System.out.println("Failed to get tamura for " + tamura_fails + " photos.");
	}
}
