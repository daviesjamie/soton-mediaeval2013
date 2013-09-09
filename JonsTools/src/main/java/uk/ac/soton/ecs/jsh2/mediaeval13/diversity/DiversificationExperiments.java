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
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.extractors.SIFTBOVW;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates.DescriptionLength;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates.FaceDetections;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates.GeoDistance;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates.HoGPedestrians;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates.NumViews;
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
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.simmat.providers.TimeOfDayDelta;
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
							new SimMatDBScanBasedDiversifier(
									new CombinedProvider(
											new AvgCombiner(),
											new TimeUser(4),
											new TimeOfDayDelta(3)
									// new
									// FeatureSim(ProvidedFeatures.TitleVector,
									// DoubleFVComparison.COSINE_SIM)
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
							new GeoDistance(10, true), new NumViews(2), new DescriptionLength(2000)
					),
					new DiversifiedScorer(
							new LuceneReranker(),
							new SimMatMaxDistGreedyDiversifier(
									new CombinedProvider(
											new AvgCombiner(),
											new TimeUser(4),
											new MonthDelta(3)// ,
									),
									AggregationStrategy.Max
							)
					)
			);
		}
	},
	bovw {
		@Override
		public Scorer get() {
			return new FilteredScorer(
					new PreFilter(
							new GeoDistance(10, true),
							new NumViews(2),
							new DescriptionLength(2000)
					// new FaceDetections(),
					// new HoGPedestrians(),
					// new Text()
					),
					new DiversifiedScorer(
							new LuceneReranker(),
							new SimMatMaxDistGreedyDiversifier(
									new CombinedProvider(
											new AvgCombiner(),
											new TimeUser(3.25),
											new MonthDelta(5),
											new FeatureSim(new SIFTBOVW(), DoubleFVComparison.COSINE_SIM)
									), AggregationStrategy.Max
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
	run1visonlyv1 {
		@Override
		public Scorer get() {
			return new DiversifiedScorer(
					// new NearDuplicatesFilter(null
					new SimMatMaxDistGreedyDiversifier(
							new FeatureSim(
									new SIFTBOVW(), DoubleFVComparison.COSINE_SIM),
							AggregationStrategy.Max
					// )
					)
			);
		}
	},
	run2textonlyv1 {
		@Override
		public Scorer get() {
			return new FilteredScorer(
					new PreFilter(
							new GeoDistance(8, true),
							new NumViews(2),
							new DescriptionLength(2000)
					),
					new DiversifiedScorer(
							new LuceneReranker(),
							new SimMatMaxDistGreedyDiversifier(
									new CombinedProvider(
											new AvgCombiner(),
											new TimeUser(3.25),
											new MonthDelta(5)
									),
									AggregationStrategy.Max
							)
					)
			);
		}
	},
	run3textvisv1 {
		@Override
		public Scorer get() {
			return new FilteredScorer(
					new PreFilter(
							new GeoDistance(8, true),
							new NumViews(2),
							new DescriptionLength(2000)
					),
					new DiversifiedScorer(
							new LuceneReranker(),
							new NearDuplicatesFilter(
									new SimMatMaxDistGreedyDiversifier(
											new CombinedProvider(
													new AvgCombiner(),
													new TimeUser(3.25),
													new MonthDelta(5)
											),
											AggregationStrategy.Max
									)
							)
					)
			);
		}
	};

	public abstract Scorer get();

	public static void main(String[] args) throws Exception {
		final DiversificationExperiments exp = DiversificationExperiments.run1visonlyv1;

		final boolean devset = true;
		final File baseDir = new File("/Users/jon/Data/mediaeval/diversity/");

		final String runId = exp.name();
		final String folder = devset ? "experiments/" : "submission/";
		final File output = new File(baseDir, folder + "me13div_SOTON-WAIS2013_" + runId + ".txt");
		DiversificationHarness.performDiversification(baseDir, devset, exp.get(), output, runId);
	}
}
