package uk.ac.soton.ecs.jsh2.mediaeval13.diversity;

import java.io.File;

import org.openimaj.feature.DoubleFVComparison;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.DBScanBasedDiversifier;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.Diversifier;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.FilteringDiversifier;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.MaxDistGreedyDiversifier;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.NoOpDiversifier;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.RandomDiversifier;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.extractors.MultiFeatures;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.extractors.ProvidedFeatures;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates.HoGPedestrians;

public enum DiversificationExperiments {
	NoOp_top50 {
		@Override
		public Diversifier get() {
			return new NoOpDiversifier();
		}
	},
	random_top50 {
		@Override
		public Diversifier get() {
			return new RandomDiversifier();
		}
	},
	ocv_peds_filtered {
		@Override
		public Diversifier get() {
			return new FilteringDiversifier(new HoGPedestrians());
		}
	},
	greedy_max_dist_tfidf_no_filter {
		@Override
		public Diversifier get() {
			return new MaxDistGreedyDiversifier(
					ProvidedFeatures.TxtTFIDF,
					DoubleFVComparison.EUCLIDEAN,
					MaxDistGreedyDiversifier.AggregationStrategy.Min
			);
		}
	},
	greedy_max_dist_allprov_no_filter {
		@Override
		public Diversifier get() {
			return new MaxDistGreedyDiversifier(
					new MultiFeatures(ProvidedFeatures.values()),
					DoubleFVComparison.EUCLIDEAN,
					MaxDistGreedyDiversifier.AggregationStrategy.Sum
			);
		}
	},
	dbscan_tfidf_cosine_no_filter_1_099 {
		@Override
		public Diversifier get() {
			return new DBScanBasedDiversifier(
					ProvidedFeatures.TxtTFIDF,
					DoubleFVComparison.COSINE_SIM,
					1,
					0.99
			);
		}
	},
	dbscan_tfidf_cosine_no_filter_1_098 {
		@Override
		public Diversifier get() {
			return new DBScanBasedDiversifier(
					ProvidedFeatures.TxtTFIDF,
					DoubleFVComparison.COSINE_SIM,
					1,
					0.98
			);
		}
	},
	dbscan_tfidf_cosine_no_filter_1_097 {
		@Override
		public Diversifier get() {
			return new DBScanBasedDiversifier(
					ProvidedFeatures.TxtTFIDF,
					DoubleFVComparison.COSINE_SIM,
					1,
					0.97
			);
		}
	},
	dbscan_cn3x3_corr_no_filter_1_070 {
		@Override
		public Diversifier get() {
			return new DBScanBasedDiversifier(
					ProvidedFeatures.VisCN3x3,
					DoubleFVComparison.CORRELATION,
					1,
					0.70
			);
		}
	},
	dbscan_vis_no_filter {
		@Override
		public Diversifier get() {
			return new DBScanBasedDiversifier(
					ProvidedFeatures.VisHOG,
					DoubleFVComparison.CORRELATION,
					1,
					0.90
			);
		}
	},
	;

	public abstract Diversifier get();

	public static void main(String[] args) throws Exception {
		final DiversificationExperiments exp = DiversificationExperiments.dbscan_cn3x3_corr_no_filter_1_070;

		final File baseDir = new File("/Users/jon/Data/mediaeval/diversity/");
		final boolean devset = true;
		final String runId = exp.name();
		final File output = new File(baseDir, "experiments/" + runId);
		DiversificationHarness.performDiversification(baseDir, devset, exp.get(), output, runId);
	}
}
