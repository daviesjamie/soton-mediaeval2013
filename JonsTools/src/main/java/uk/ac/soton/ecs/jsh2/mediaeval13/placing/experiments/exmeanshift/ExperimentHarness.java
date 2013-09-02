package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift;

import java.io.File;

import org.apache.lucene.search.IndexSearcher;
import org.openimaj.experiment.ExperimentContext;
import org.openimaj.experiment.ExperimentRunner;
import org.openimaj.experiment.RunnableExperiment;
import org.openimaj.experiment.agent.ExperimentAgent;
import org.openimaj.experiment.annotations.IndependentVariable;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.CachingTagBasedEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.PriorEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.ScoreWeightedVisualEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.VisualEstimator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.LSHSiftGraphSearcher;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.util.Utils;

public class ExperimentHarness {
	private static File DEFAULT_LUCENE_INDEX = new File("/Volumes/SSD/mediaeval13/placing/places.lucene");
	private static final File DEFAULT_LAT_LNG_FILE = new File("/Volumes/SSD/mediaeval13/placing/training_latlng");
	private static final File DEFAULT_CACHE_LOCATION = new File("/Volumes/SSD/tags-cache/");

	public enum Experiments {
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
		LshOnly {
			@IndependentVariable
			int lshMinEdgeCount = 1;

			@IndependentVariable
			boolean lshExpand = false;

			@IndependentVariable
			File lshEdges = new File("/Volumes/SSD/mediaeval13/placing/sift1x-dups/edges-v2.txt");

			@Override
			protected RunnableExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(lshEdges, lshMinEdgeCount, luceneIndex);
				lsh.setExpand(lshExpand);

				return new MeanShiftPlacingExperiment(0.01, 1000,
						new VisualEstimator(luceneIndex, lsh, 100000));
			}
		},
		ScoreWeightedLshOnly {
			@IndependentVariable
			File lshEdges = new File("/Volumes/SSD/mediaeval13/placing/sift1x-dups/edges-v2.txt");

			@Override
			protected RunnableExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(lshEdges, 1, luceneIndex);
				lsh.setExpand(false);

				return new MeanShiftPlacingExperiment(0.01, 1000,
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f));
			}
		},
		ScoreWeightedLshAndTagsAndPrior {
			@IndependentVariable
			File lshEdges = new File("/Volumes/SSD/mediaeval13/placing/sift1x-dups/edges-v2.txt");

			@Override
			protected RunnableExperiment create() throws Exception {
				final IndexSearcher luceneIndex = Utils.loadLuceneIndex(DEFAULT_LUCENE_INDEX);
				final LSHSiftGraphSearcher lsh = new LSHSiftGraphSearcher(lshEdges, 1, luceneIndex);
				lsh.setExpand(false);

				return new MeanShiftPlacingExperiment(0.01, 1000,
						new PriorEstimator(DEFAULT_LAT_LNG_FILE),
						new CachingTagBasedEstimator(luceneIndex, DEFAULT_CACHE_LOCATION),
						new ScoreWeightedVisualEstimator(luceneIndex, lsh, 100000, 1.0f));
			}
		},
		;

		protected abstract RunnableExperiment create() throws Exception;
	}

	public static void main(String[] args) throws Exception {
		ExperimentAgent.initialise();

		final RunnableExperiment exp = Experiments.ScoreWeightedLshAndTagsAndPrior.create();

		final ExperimentContext ctx = ExperimentRunner.runExperiment(exp);

		System.out.println(ctx);
	}
}
