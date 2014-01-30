package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongProcedure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.search.IndexSearcher;
import org.openimaj.experiment.ExperimentContext;
import org.openimaj.experiment.ExperimentRunner;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.BoostingCachingTagBasedEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.CachingTagBasedEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.PriorEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.ScoreWeightedVisualEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.InMemCEDDPQSearcher;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.LSHSiftGraphSearcher;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.VLADSearcher;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.util.Utils;

public class ICMRHarness {
	private static final File BASE = new File("/Volumes/SSD/mediaeval13/placing/");

	private static final File DEFAULT_LUCENE_INDEX = new File(BASE, "placesutf8.lucene");
	private static final File DEFAULT_LAT_LNG_FILE = new File(BASE, "training_latlng");
	private static final File DEFAULT_CACHE_LOCATION = new File(BASE, "caches");
	private static final File DEFAULT_LSH_EDGES_FILE = new File(BASE, "sift1x-dups/sift1x-lsh-edges-min1-max20.txt");
	private static final File DEFAULT_VLAD_INDEX = new File(BASE, "vlad-indexes/rgb-sift1x-vlad64n-pca128-pq16-adcnn.idx");
	private static final File DEFAULT_VLAD_FEATURES_FILE = new File(BASE, "vlad-indexes/rgb-sift1x-vlad64n-pca128.dat");
	private static final File DEFAULT_LIRE_FEATURE_LOCATION = new File(BASE, "features");

	private static final File BIG_SET_LUCENE_INDEX = new File(BASE, "bigdatasetutf8-3.lucene");
	private static final File BIG_SET_CACHE_LOCATION = new File(BASE, "data/bigcache");
	private static final File BIG_SET_LAT_LNG_FILE = new File(BASE, "big_latlng");
	private static final File BIG_SET_LSH_EDGES_FILE = new File(BASE, "sift1x-dups/big-sift1x-lsh-edges-min1-max20.txt");

	private static final File BIG_SET_WUSERS_LUCENE_INDEX = new File(BASE, "bigdatasetutf8-withusers.lucene");
	private static final File BIG_SET_WUSERS_CACHE_LOCATION = new File(BASE, "data/big-withusers-cache");
	private static final File BIG_SET_WUSERS_LAT_LNG_FILE = new File(BASE, "big_latlng");
	private static final File BIG_SET_WUSERS_LSH_EDGES_FILE = new File(BASE,
			"sift1x-dups/big-sift1x-lsh-edges-min1-max20.txt");

	private static final File BIG_SET_WUSERS24_LUCENE_INDEX = new File(BASE, "bigdatasetutf8-withusers-24hrs.lucene");
	private static final File BIG_SET_WUSERS24_CACHE_LOCATION = new File(BASE, "data/big-withusers-24hrs-cache");
	private static final File BIG_SET_WUSERS24_LAT_LNG_FILE = new File(BASE, "big_latlng");
	private static final File BIG_SET_WUSERS24_LSH_EDGES_FILE = new File(BASE,
			"sift1x-dups/big-sift1x-lsh-edges-min1-max20.txt");

	private static final File TEST_SET_FILE = new File(BASE, "testset.csv");
	private static final File TEST_SET_GT_FILE = new File(BASE, "test_latlng");

	private static final URL TEST_SET_URL;
	private static final URL TEST_SET_GT_URL;

	private static final File RESULTS_DIR = new File(BASE, "/icmr-results-sinusoidal/");

	static {
		try {
			TEST_SET_URL = TEST_SET_FILE.toURI().toURL();
			TEST_SET_GT_URL = TEST_SET_GT_FILE.toURI().toURL();
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public enum Runs {
		MEPriorOnly {
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE));
			}
		},
		METagsOnly {
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION));
			}
		},
		MEBoostedTagsOnly {
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new BoostingCachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION));
			}
		},
		MECEDDOnly {
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);

				final File ceddData = new File(DEFAULT_LIRE_FEATURE_LOCATION, "cedd.bin");
				final InMemCEDDPQSearcher cedd = new InMemCEDDPQSearcher(ceddData, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new ScoreWeightedVisualEstimator(luceneIndex, cedd, 100, 1.0f));
			}
		},
		MEVLADOnly {
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);

				final VLADSearcher vlad = new VLADSearcher(DEFAULT_VLAD_FEATURES_FILE, DEFAULT_VLAD_INDEX, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new ScoreWeightedVisualEstimator(luceneIndex, vlad, 100, 1.0f));
			}
		},
		MEVLADOnlyT10 {
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);

				final VLADSearcher vlad = new VLADSearcher(DEFAULT_VLAD_FEATURES_FILE, DEFAULT_VLAD_INDEX, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new ScoreWeightedVisualEstimator(luceneIndex, vlad, 10, 1.0f));
			}
		},
		MEVLADOnlyT50 {
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);

				final VLADSearcher vlad = new VLADSearcher(DEFAULT_VLAD_FEATURES_FILE, DEFAULT_VLAD_INDEX, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new ScoreWeightedVisualEstimator(luceneIndex, vlad, 50, 1.0f));
			}
		},
		MEVLADOnlyT150 {
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);

				final VLADSearcher vlad = new VLADSearcher(DEFAULT_VLAD_FEATURES_FILE, DEFAULT_VLAD_INDEX, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new ScoreWeightedVisualEstimator(luceneIndex, vlad, 150, 1.0f));
			}
		},
		MEVLADOnlyT200 {
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);

				final VLADSearcher vlad = new VLADSearcher(DEFAULT_VLAD_FEATURES_FILE, DEFAULT_VLAD_INDEX, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new ScoreWeightedVisualEstimator(luceneIndex, vlad, 200, 1.0f));
			}
		},
		MEVLADOnlyT250 {
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);

				final VLADSearcher vlad = new VLADSearcher(DEFAULT_VLAD_FEATURES_FILE, DEFAULT_VLAD_INDEX, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new ScoreWeightedVisualEstimator(luceneIndex, vlad, 250, 1.0f));
			}
		},
		MELSHSIFTOnly {
			// Text + Visual
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f));
			}
		},
		MediaEval_R1 {
			// Text + Visual
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				final File ceddData = new File(DEFAULT_LIRE_FEATURE_LOCATION, "cedd.bin");
				final InMemCEDDPQSearcher cedd = new InMemCEDDPQSearcher(ceddData, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f),
						new ScoreWeightedVisualEstimator(luceneIndex, cedd, 100, 1.0f));
			}
		},
		MediaEval_R1_NoCEDD {
			// Text + Visual
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f));
			}
		},
		MediaEval_R1_VLAD {
			// Text + Visual
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				final VLADSearcher vlad = new VLADSearcher(DEFAULT_VLAD_FEATURES_FILE, DEFAULT_VLAD_INDEX, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f),
						new ScoreWeightedVisualEstimator(luceneIndex, vlad, 100, 1.0f));
			}
		},
		MediaEval_R2 {
			// Visual Only
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				final File ceddData = new File(DEFAULT_LIRE_FEATURE_LOCATION, "cedd.bin");
				final InMemCEDDPQSearcher cedd = new InMemCEDDPQSearcher(ceddData, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f),
						new ScoreWeightedVisualEstimator(luceneIndex, cedd, 100, 1.0f));
			}
		},
		VLAD_LSH {
			// Visual Only
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				final VLADSearcher vlad = new VLADSearcher(DEFAULT_VLAD_FEATURES_FILE, DEFAULT_VLAD_INDEX, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f),
						new ScoreWeightedVisualEstimator(luceneIndex, vlad, 100, 1.0f));
			}
		},
		MediaEval_R3 {
			// Textual Only
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION));
			}
		},
		MediaEval_R4 {
			// Text + Visual, Big data
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(BIG_SET_LUCENE_INDEX);

				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(BIG_SET_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				// final VLADSearcher vlad = new
				// VLADSearcher(BIG_SET_VLAD_FEATURES_FILE, BIG_SET_VLAD_INDEX,
				// luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(BIG_SET_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, BIG_SET_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f)// ,
				// new ScoreWeightedVisualEstimator(luceneIndex, vlad, 100,
				// 1.0f)
				);
			}
		},
		MediaEval_R4_TAGS {
			// Text + Visual, Big data
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(BIG_SET_LUCENE_INDEX);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(BIG_SET_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, BIG_SET_CACHE_LOCATION)
				);
			}
		},
		MediaEval_R4_VISUAL {
			// Text + Visual, Big data
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(BIG_SET_LUCENE_INDEX);

				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(BIG_SET_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(BIG_SET_LAT_LNG_FILE),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f)// ,
				// new ScoreWeightedVisualEstimator(luceneIndex, vlad, 100,
				// 1.0f)
				);
			}
		},
		MediaEval_R5 {
			// Text + Visual, no prior
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				final File ceddData = new File(DEFAULT_LIRE_FEATURE_LOCATION, "cedd.bin");
				final InMemCEDDPQSearcher cedd = new InMemCEDDPQSearcher(ceddData, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new BoostingCachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f),
						new ScoreWeightedVisualEstimator(luceneIndex, cedd, 100, 1.0f));
			}
		},
		BTAGS_LSH_VLAD_PRIOR {
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				final VLADSearcher vlad = new VLADSearcher(DEFAULT_VLAD_FEATURES_FILE, DEFAULT_VLAD_INDEX, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(BIG_SET_LAT_LNG_FILE),
						new BoostingCachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f),
						new ScoreWeightedVisualEstimator(luceneIndex, vlad, 100, 1.0f));
			}
		},
		MediaEval_R4_WITH_COMPLETE_USERS {
			// Text + Visual, Big data
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(BIG_SET_WUSERS_LUCENE_INDEX);

				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(BIG_SET_WUSERS_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				// final VLADSearcher vlad = new
				// VLADSearcher(BIG_SET_WUSERS_VLAD_FEATURES_FILE,
				// BIG_SET_WUSERS_VLAD_INDEX,
				// luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(BIG_SET_WUSERS_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, BIG_SET_WUSERS_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f)// ,
				// new ScoreWeightedVisualEstimator(luceneIndex, vlad, 100,
				// 1.0f)
				);
			}
		},
		MediaEval_R4_WITH_COMPLETE_USERS_TAGS {
			// Text + Visual, Big data
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(BIG_SET_WUSERS_LUCENE_INDEX);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(BIG_SET_WUSERS_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, BIG_SET_WUSERS_CACHE_LOCATION)
				);
			}
		},
		MediaEval_R4_WITH_COMPLETE_USERS_VISUAL {
			// Text + Visual, Big data
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(BIG_SET_WUSERS_LUCENE_INDEX);

				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(BIG_SET_WUSERS_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(BIG_SET_WUSERS_LAT_LNG_FILE),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f)// ,
				);
			}
		},
		MediaEval_R4_WITH_24HR_FILTERED_USERS {
			// Text + Visual, Big data
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(BIG_SET_WUSERS24_LUCENE_INDEX);

				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(BIG_SET_WUSERS24_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				// final VLADSearcher vlad = new
				// VLADSearcher(BIG_SET_WUSERS_VLAD_FEATURES_FILE,
				// BIG_SET_WUSERS_VLAD_INDEX,
				// luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(BIG_SET_WUSERS24_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, BIG_SET_WUSERS24_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f)// ,
				// new ScoreWeightedVisualEstimator(luceneIndex, vlad, 100,
				// 1.0f)
				);
			}
		},
		MediaEval_R4_WITH_24HR_FILTERED_USERS_TAGS {
			// Text + Visual, Big data
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(BIG_SET_WUSERS24_LUCENE_INDEX);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(BIG_SET_WUSERS24_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, BIG_SET_WUSERS24_CACHE_LOCATION)
				);
			}
		},
		MediaEval_R4_WITH_24HR_FILTERED_USERS_VISUAL {
			// Text + Visual, Big data
			@Override
			protected MeanShiftPlacingExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(BIG_SET_WUSERS24_LUCENE_INDEX);

				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(BIG_SET_WUSERS24_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				return new MeanShiftPlacingExperiment(0.01, 1000, TEST_SET_URL, TEST_SET_GT_URL,
						new PriorEstimator(BIG_SET_WUSERS24_LAT_LNG_FILE),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f)
				);
			}
		},
		;

		protected abstract MeanShiftPlacingExperiment create() throws Exception;
	}

	private static void writeRawResults(File outputFile, final TLongObjectHashMap<GeoLocationEstimate> results)
			throws IOException
	{
		final BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));

		results.forEachKey(new TLongProcedure() {
			@Override
			public boolean execute(long value) {
				final GeoLocationEstimate r = results.get(value);
				try {
					out.write(String.valueOf(value));
					out.write(';');
					out.write(String.valueOf(r.latitude));
					out.write(';');
					out.write(String.valueOf(r.longitude));
					out.write(';');
					out.write(String.valueOf(r.estimatedError));
					out.write('\n');
				} catch (final IOException e) {
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

	public static void main(String[] args) throws Exception {
		RESULTS_DIR.mkdirs();

		for (final Runs exp : Runs.values())
		{
			final File rawResultFile = new File(RESULTS_DIR, exp.name() + "-raw.txt");
			final File resultFile = new File(RESULTS_DIR, exp.name() + ".txt");

			if (rawResultFile.exists())
				continue;

			final MeanShiftPlacingExperiment expr = exp.create();
			final ExperimentContext ctx = ExperimentRunner.runExperiment(expr);

			final TLongObjectHashMap<GeoLocationEstimate> rawResults = expr.getRawResult();
			writeRawResults(rawResultFile, rawResults);

			FileUtils.write(resultFile, ctx.toString());
		}
	}
}
