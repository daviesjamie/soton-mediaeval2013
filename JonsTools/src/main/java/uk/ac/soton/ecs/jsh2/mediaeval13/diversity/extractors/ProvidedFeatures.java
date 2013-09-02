package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.extractors;

import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;

public enum ProvidedFeatures implements FeatureExtractor<DoubleFV, ResultItem> {
	TxtProbabilistic {
		@Override
		public DoubleFV extractFeature(ResultItem object) {
			return object.getProbabilisticVector();
		}
	},
	TxtTFIDF {
		@Override
		public DoubleFV extractFeature(ResultItem object) {
			return object.getTFIDFVector();
		}
	},
	TxtSocialTFIDF {
		@Override
		public DoubleFV extractFeature(ResultItem object) {
			return object.getSocialTFIDFVector();
		}
	},
	VisCM {
		@Override
		public DoubleFV extractFeature(ResultItem object) {
			return object.getCM();
		}
	},
	VisCM3x3 {
		@Override
		public DoubleFV extractFeature(ResultItem object) {
			return object.getCM3x3();
		}
	},
	VisCN {
		@Override
		public DoubleFV extractFeature(ResultItem object) {
			return object.getCN();
		}
	},
	VisCN3x3 {
		@Override
		public DoubleFV extractFeature(ResultItem object) {
			return object.getCN3x3();
		}
	},
	VisCSD {
		@Override
		public DoubleFV extractFeature(ResultItem object) {
			return object.getCSD();
		}
	},
	VisGLRLM {
		@Override
		public DoubleFV extractFeature(ResultItem object) {
			return object.getGLRLM();
		}
	},
	VisGLRLM3x3 {
		@Override
		public DoubleFV extractFeature(ResultItem object) {
			return object.getGLRLM3x3();
		}
	},
	VisHOG {
		@Override
		public DoubleFV extractFeature(ResultItem object) {
			return object.getHOG();
		}
	},
	VisLBP {
		@Override
		public DoubleFV extractFeature(ResultItem object) {
			return object.getLBP();
		}
	},
	VisLBP3x3 {
		@Override
		public DoubleFV extractFeature(ResultItem object) {
			return object.getLBP3x3();
		}
	};
}
