package uk.ac.soton.ecs.jsh2.mediaeval13.diversity;

import java.io.File;

import org.openimaj.feature.DoubleFVComparison;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.DiversifiedScorer;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.NearDuplicatesFilter;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.RandomDiversifier;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.SimMatDBScanBasedDiversifier;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.SimMatMaxDistGreedyDiversifier;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.SimMatMaxDistGreedyDiversifier.AggregationStrategy;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.TimeUserDiversifier;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.extractors.ProvidedFeatures;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates.FaceDetections;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates.GeoDistance;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates.HoGPedestrians;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates.PreFilter;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates.Text;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.scoring.FilteredScorer;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.scoring.LuceneReranker;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.scoring.RankScorer;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.scoring.Scorer;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.AvgCombiner;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.CombinedProvider;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.providers.FeatureSim;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.providers.MonthDelta;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.providers.TimeUser;

public enum DiversificationExperiments {
	NoOp_top50 {
		@Override
		public Scorer get() {
			return new RankScorer();
		}
	},
	random_top50 {
		@Override
		public Scorer get() {
			return new DiversifiedScorer(new RandomDiversifier());
		}
	},
	ocv_peds_filtered {
		@Override
		public Scorer get() {
			return new FilteredScorer(new PreFilter(new HoGPedestrians()));
		}
	},
	geofilter5km {
		@Override
		public Scorer get() {
			return new FilteredScorer(new PreFilter((new GeoDistance(5.0, true))));
		}
	},
	facefiltering {
		@Override
		public Scorer get() {
			return new FilteredScorer(new PreFilter(
					new FaceDetections()
					));
		}
	},
	luceneRerank {
		@Override
		public Scorer get() {
			return new LuceneReranker();
		}
	},
	filteredLuceneRerank {
		@SuppressWarnings("unchecked")
		@Override
		public Scorer get() {
			return new FilteredScorer(
					new PreFilter(
							new GeoDistance(10, true),
							new FaceDetections(),
							new HoGPedestrians()
					),
					new LuceneReranker()
			);
		}
	},
	metaOnlyDiversifier {
		@Override
		public Scorer get() {
			return new FilteredScorer(
					new PreFilter(
							new GeoDistance(10, true)
					),
					new DiversifiedScorer(
							new LuceneReranker(),
							// new TimeUserDiversifier(4, 0.1)
							new SimMatDBScanBasedDiversifier(
									new CombinedProvider(
											new AvgCombiner(),
											new TimeUser(4),
											new FeatureSim(ProvidedFeatures.TitleVector,
													DoubleFVComparison.COSINE_SIM)
									// new MonthDelta()
									// new GeoDelta(0.001)
									),
									1, 0.1, false
							)
					)
			);
		}
	},
	metaOnlyDiversifierMD {
		@Override
		public Scorer get() {
			return new FilteredScorer(
					new PreFilter(
							new GeoDistance(10, true)
					),
					new DiversifiedScorer(
							new LuceneReranker(),
							// new TimeUserDiversifier(4, 0.1)
							new SimMatMaxDistGreedyDiversifier(
									new CombinedProvider(
											new AvgCombiner(),
											new TimeUser(4),
											// new
											// FeatureSim(ProvidedFeatures.TitleVector,
											// DoubleFVComparison.COSINE_SIM),
											new MonthDelta(3)// ,
									// new User()
									// new GeoDelta(0.0001)
									),
									AggregationStrategy.Max
							)
					)
			);
		}
	},
	dupsRemoved {
		@Override
		public Scorer get() {
			return new FilteredScorer(
					new PreFilter(
							new GeoDistance(10, true),
							new Text()
					),
					new DiversifiedScorer(
							new LuceneReranker(),
							new NearDuplicatesFilter(
									new TimeUserDiversifier(4, 0.1)
							)
					)
			);
		}
	},
	;

	public abstract Scorer get();

	public static void main(String[] args) throws Exception {
		final DiversificationExperiments exp = DiversificationExperiments.metaOnlyDiversifierMD;

		final File baseDir = new File("/Users/jon/Data/mediaeval/diversity/");
		final boolean devset = true;
		final String runId = exp.name();
		final File output = new File(baseDir, "experiments/" + runId);
		DiversificationHarness.performDiversification(baseDir, devset, exp.get(), output, runId);
	}
}
