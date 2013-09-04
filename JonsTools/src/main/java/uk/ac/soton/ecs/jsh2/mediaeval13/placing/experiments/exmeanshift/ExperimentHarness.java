package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.search.IndexSearcher;
import org.openimaj.experiment.ExperimentContext;
import org.openimaj.experiment.ExperimentRunner;
import org.openimaj.experiment.RunnableExperiment;
import org.openimaj.experiment.agent.ExperimentAgent;
import org.openimaj.experiment.annotations.IndependentVariable;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.CachingTagBasedEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.PriorEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.RandomEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.ScoreWeightedVisualEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.UploadedBasedEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.VisualEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.InMemCEDDPQSearcher;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.LSHSiftGraphSearcher;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.VLADSearcher;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.util.Utils;

public class ExperimentHarness {
	static {
		try {
			ExperimentAgent.initialise();
		} catch (final IOException e) {
			throw new AssertionError(e);
		}
	}

	private static File DEFAULT_LUCENE_INDEX = new File("/Volumes/SSD/mediaeval13/placing/places.lucene");
	private static final File DEFAULT_LAT_LNG_FILE = new File("/Volumes/SSD/mediaeval13/placing/training_latlng");
	private static final File DEFAULT_CACHE_LOCATION = new File("/Volumes/SSD/tags-cache/");
	private static final File DEFAULT_LSH_EDGES_FILE = new File("/Volumes/SSD/mediaeval13/placing/sift1x-dups/sift1x-lsh-edges-min1-max20.txt");
	private static final File DEFAULT_VLAD_INDEX = new File("/Volumes/SSD/mediaeval13/placing/vlad-indexes/rgb-sift1x-vlad64n-pca128-pq16-adcnn.idx");
	private static final File DEFAULT_VLAD_FEATURES_FILE = new File("/Volumes/SSD/mediaeval13/placing/vlad-indexes/rgb-sift1x-vlad64n-pca128.dat");

	public enum Experiments {
		Random {
			@Override
			protected RunnableExperiment create() throws Exception {
				return new MeanShiftPlacingExperiment( 0.01, 1000,
						new RandomEstimator());
			}
		},
		PriorOnly {
			@Override
			protected RunnableExperiment create() throws Exception {
				return new MeanShiftPlacingExperiment(0.01, 1000,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE));
			}
		},
		TagsOnly {
			@Override
			protected RunnableExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);

				return new MeanShiftPlacingExperiment(0.01, 1000,
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION));
			}
		},
		TagsAndPrior {
			@Override
			protected RunnableExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);

				return new MeanShiftPlacingExperiment(0.01, 1000,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION));
			}
		},
		UploadedOnly {
			@Override
			protected RunnableExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);

				return new MeanShiftPlacingExperiment(0.01, 1000,
						new UploadedBasedEstimator(luceneIndex));
			}
		},
		UploadedAndTagsAndPrior {
			@Override
			protected RunnableExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);

				return new MeanShiftPlacingExperiment(0.01, 1000,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new UploadedBasedEstimator(luceneIndex),
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION));
			}
		},
		LshOnly {
			@Override
			protected RunnableExperiment create() throws Exception {
				final int lshMinEdgeCountValue = 1;
				final boolean lshExpandValue = false;

				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, lshMinEdgeCountValue, luceneIndex);
				lsh.setExpand(lshExpandValue);

				return new MeanShiftPlacingExperiment(0.01, 1000,
						new VisualEstimator(luceneIndex, lsh, 100000))
				{
					@IndependentVariable
					int lshMinEdgeCount = lshMinEdgeCountValue;

					@IndependentVariable
					boolean lshExpand = lshExpandValue;

					@IndependentVariable
					File lshEdges = DEFAULT_LSH_EDGES_FILE;
				};
			}
		},
		ScoreWeightedLshOnly {
			@Override
			protected RunnableExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				return new MeanShiftPlacingExperiment(0.01, 1000,
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f))
				{
					@IndependentVariable
					File lshEdges = DEFAULT_LSH_EDGES_FILE;
				};
			}
		},
		ScoreWeightedLshAndTags {
			@Override
			protected RunnableExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				return new MeanShiftPlacingExperiment(0.01, 1000,
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f))
				{
					@IndependentVariable
					File lshEdges = DEFAULT_LSH_EDGES_FILE;
				};
			}
		},
		ScoreWeightedLshAndTagsAndPrior {
			@Override
			protected RunnableExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				return new MeanShiftPlacingExperiment(0.01, 1000,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f))
				{
					@IndependentVariable
					File lshEdges = DEFAULT_LSH_EDGES_FILE;
				};
			}
		},
		ScoreWeightedLshAndCEDD100AndTagsAndPrior {
			@Override
			protected RunnableExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				final File ceddData = new File("/Volumes/SSD/mediaeval13/placing/cedd.bin");
				final InMemCEDDPQSearcher cedd = new InMemCEDDPQSearcher(ceddData, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f),
						new ScoreWeightedVisualEstimator(luceneIndex, cedd, 100, 1.0f)
				)
				{
					@IndependentVariable
					File lshEdges = DEFAULT_LSH_EDGES_FILE;

					@IndependentVariable
					File ceddFile = ceddData;
				};
			}
		},
		ScoreWeightedLshAndCEDD10AndTagsAndPrior {
			@Override
			protected RunnableExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				final File ceddData = new File("/Volumes/SSD/mediaeval13/placing/cedd.bin");
				final InMemCEDDPQSearcher cedd = new InMemCEDDPQSearcher(ceddData, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f),
						new ScoreWeightedVisualEstimator(luceneIndex, cedd, 10, 1.0f)
				)
				{
					@IndependentVariable
					File lshEdges = DEFAULT_LSH_EDGES_FILE;

					@IndependentVariable
					File ceddFile = ceddData;
				};
			}
		},
		ScoreWeightedLshAndPQVLADAndTagsAndPrior {
			@Override
			protected RunnableExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(DEFAULT_LSH_EDGES_FILE, 1, luceneIndex);
				lsh.setExpand(false);

				final VLADSearcher vlad = new VLADSearcher(DEFAULT_VLAD_FEATURES_FILE, DEFAULT_VLAD_INDEX, luceneIndex);

				return new MeanShiftPlacingExperiment(0.01, 1000,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f),
						new ScoreWeightedVisualEstimator(luceneIndex, vlad, 100, 1.0f)
				)
				{
					@IndependentVariable
					File lshEdges = DEFAULT_LSH_EDGES_FILE;

					@IndependentVariable
					File vladIndexFile = DEFAULT_VLAD_INDEX;

					@IndependentVariable
					File vladFeatureFile = DEFAULT_VLAD_FEATURES_FILE;
				};
			}
		},
		;

		protected abstract RunnableExperiment create() throws Exception;
	}

	public static void main(String[] args) throws Exception {
		final Experiments exp = Experiments.ScoreWeightedLshAndPQVLADAndTagsAndPrior;

		final RunnableExperiment expr = exp.create();
		final ExperimentContext ctx = ExperimentRunner.runExperiment(expr);

		System.out.println(ctx);

		final File report = new File("/Volumes/SSD/mediaeval13/placing/reports/" + exp.name() + ".txt");
		report.getParentFile().mkdirs();
		FileUtils.write(report, ctx.toString());
	}
}
