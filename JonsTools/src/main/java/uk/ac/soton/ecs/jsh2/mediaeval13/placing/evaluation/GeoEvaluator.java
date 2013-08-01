package uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongObjectProcedure;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.experiment.evaluation.Evaluator;
import org.openimaj.util.pair.DoubleDoublePair;

public class GeoEvaluator implements Evaluator<TLongObjectHashMap<GeoLocationEstimate>, GeoAnalysisResult> {
	TLongObjectHashMap<GeoLocation> groundTruth;

	@Override
	public TLongObjectHashMap<GeoLocationEstimate> evaluate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GeoAnalysisResult analyse(TLongObjectHashMap<GeoLocationEstimate> rawData) {
		final List<DoubleDoublePair> results = new ArrayList<DoubleDoublePair>(rawData.size());

		rawData.forEachEntry(new TLongObjectProcedure<GeoLocationEstimate>() {
			@Override
			public boolean execute(long id, GeoLocationEstimate estimate) {
				final GeoLocation actual = groundTruth.get(id);
				final double actualError = estimate.haversine(actual);

				results.add(new DoubleDoublePair(actualError, estimate.estimatedError));

				return true;
			}
		});

		return new GeoAnalysisResult(results);
	}

}
