package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongProcedure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.lucene.search.IndexSearcher;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.CachingTagBasedEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.PriorEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.ScoreWeightedVisualEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.InMemCEDDPQSearcher;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.LSHSiftGraphSearcher;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.util.Utils;

public class RunHarness {
	private static final File BASE = new File(".");

	private static final File DEFAULT_LUCENE_INDEX = new File(BASE, "placesutf8.lucene");
	private static final File DEFAULT_LAT_LNG_FILE = new File(BASE, "training_latlng");
	private static final File DEFAULT_CACHE_LOCATION = new File(BASE, "caches");
	private static final File DEFAULT_LSH_EDGES_FILE = new File(BASE, "sift1x-dups/sift1x-lsh-edges-min1-max20.txt");
	private static final File DEFAULT_VLAD_INDEX = new File(BASE, "vlad-indexes/rgb-sift1x-vlad64n-pca128-pq16-adcnn.idx");
	private static final File DEFAULT_VLAD_FEATURES_FILE = new File(BASE, "vlad-indexes/rgb-sift1x-vlad64n-pca128.dat");
	private static final File DEFAULT_LIRE_FEATURE_LOCATION = new File(BASE, "features");

	private static final File BIG_SET_LUCENE_INDEX = new File(BASE, "bigdataset2.lucene");
	private static final File BIG_SET_CACHE_LOCATION = new File(BASE, "bigcache");
	private static final File BIG_SET_LAT_LNG_FILE = new File(BASE, "big_latlng");

	private static final File TEST_SET_FILE = new File(BASE, "testset.csv");

	public enum Runs {
		Run1 {
			// Text + Visual
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				final File ceddData = new File(DEFAULT_LIRE_FEATURE_LOCATION, "cedd.bin");
				final InMemCEDDPQSearcher cedd = new InMemCEDDPQSearcher(ceddData, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_FILE.toURI().toURL(), null,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f),
						new ScoreWeightedVisualEstimator(luceneIndex, cedd, 100, 1.0f));
			}
		},
		Run2 {
			// Visual Only
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				final File ceddData = new File(DEFAULT_LIRE_FEATURE_LOCATION, "cedd.bin");
				final InMemCEDDPQSearcher cedd = new InMemCEDDPQSearcher(ceddData, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_FILE.toURI().toURL(), null,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f),
						new ScoreWeightedVisualEstimator(luceneIndex, cedd, 100, 1.0f));
			}
		},
		Run3 {
			// Textual Only
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_FILE.toURI().toURL(), null,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION));
			}
		},
		Run4 {
			// Text + Visual, Big data
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(BIG_SET_LUCENE_INDEX);
				
				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_FILE.toURI().toURL(), null,
						new PriorEstimator(BIG_SET_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, BIG_SET_CACHE_LOCATION));
				
				// TODO: Add visual features!
				// TODO: Filter test set users from data set
			}
		},
		Run5 {
			// Text + Visual, no prior
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				final File ceddData = new File(DEFAULT_LIRE_FEATURE_LOCATION, "cedd.bin");
				final InMemCEDDPQSearcher cedd = new InMemCEDDPQSearcher(ceddData, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_FILE.toURI().toURL(), null,
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f),
						new ScoreWeightedVisualEstimator(luceneIndex, cedd, 100, 1.0f));
			}
		};

		protected abstract MeanShiftPlacingExperiment create() throws Exception;
	}

	public static void main(String[] args) throws Exception {
		Runs rrun = null;
		switch(Integer.parseInt(args[0])) {
			case 1:
				rrun = Runs.Run1;
				break;
			case 2:
				rrun = Runs.Run2;
				break;
			case 3:
				rrun = Runs.Run3;
				break;
			case 4:
				rrun = Runs.Run4;
				break;
			case 5:
				rrun = Runs.Run5;
				break;
			default:
				System.err.println(args[0] + " is an invalid run number! Try the integers 1-5");
				System.exit(1);
		}

		final File resultOutputDir = new File(args[1]);

		final MeanShiftPlacingExperiment run = rrun.create();
		run.perform();
		final TLongObjectHashMap<GeoLocationEstimate> results = run.getRawResult();

		final File outputFile = new File(resultOutputDir, rrun.toString());
		final BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));

		results.forEachKey(new TLongProcedure() {
			@Override
			public boolean execute(long value) {
				GeoLocationEstimate r = results.get(value);
				try {
					out.write(String.valueOf(value));
					out.write(';');
					out.write(String.valueOf(r.latitude));
					out.write(';');
					out.write(String.valueOf(r.longitude));
					out.write(';');
					out.write(String.valueOf(r.estimatedError));
					out.write('\n');
				} catch( IOException e ) {
					System.err.println("Failed to write out results!");
					e.printStackTrace();
					return false;
				}
				
				return true;
			}
		});
		
		out.flush();
		out.close();
		
		System.out.println("Done!");
		System.out.println("Output written to " + outputFile);
	}
}
