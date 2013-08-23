package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.experiment.ExperimentContext;
import org.openimaj.experiment.RunnableExperiment;
import org.openimaj.experiment.annotations.DependentVariable;
import org.openimaj.experiment.annotations.IndependentVariable;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoAnalysisResult;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoEvaluator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoPositioningEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers.GeoDensityEstimateProvider;

public class MeanShiftPlacingExperiment implements RunnableExperiment {
	@IndependentVariable
	private GeoDensityEstimateProvider[] providers;

	@IndependentVariable
	private double bandwidth;

	@IndependentVariable
	private int sampleCount;

	@IndependentVariable
	private URL queriesLocation;

	@IndependentVariable
	private URL groundTruthLocation;

	@DependentVariable
	private GeoAnalysisResult result;

	private TLongObjectHashMap<GeoLocation> groundTruth;
	private TLongArrayList skipIds;
	private List<QueryImageData> queries;

	public MeanShiftPlacingExperiment(double bandwidth, int sampleCount,
			URL queries, URL groundTruthLocation, GeoDensityEstimateProvider... providers) throws IOException
	{
		this.providers = providers;
		this.bandwidth = bandwidth;
		this.sampleCount = sampleCount;
		this.queriesLocation = queries;
		this.groundTruthLocation = groundTruthLocation;

		this.queries = readQueries(queriesLocation);
		this.skipIds = getSkipIds(this.queries);
		this.groundTruth = GeoEvaluator.readGroundTruth(groundTruthLocation.openStream());

		for (final GeoDensityEstimateProvider p : providers)
			p.setSkipIds(skipIds);
	}

	public MeanShiftPlacingExperiment(double bandwidth, int sampleCount, GeoDensityEstimateProvider... providers)
			throws IOException
	{
		this(bandwidth, sampleCount,
				MeanShiftPlacingExperiment.class
						.getResource("/uk/ac/soton/ecs/jsh2/mediaeval13/placing/data/validation.csv"),
				MeanShiftPlacingExperiment.class
						.getResource("/uk/ac/soton/ecs/jsh2/mediaeval13/placing/data/validation_latlng"),
				providers);
	}

	@Override
	public void setup() {
		// do nothing
	}

	@Override
	public void perform() {
		final GeoPositioningEngine engine = new ExtensibleMeanShiftEngine(sampleCount, bandwidth, providers);

		final GeoEvaluator eval = new GeoEvaluator(groundTruth, engine, queries);

		this.result = eval.analyse(eval.evaluate());
	}

	@Override
	public void finish(ExperimentContext context) {
		// do nothing
	}

	private static TLongArrayList getSkipIds(List<QueryImageData> queries) {
		final TLongArrayList ids = new TLongArrayList(queries.size());

		for (final QueryImageData q : queries)
			ids.add(q.flickrId);

		return ids;
	}

	private static List<QueryImageData> readQueries(URL is) throws IOException {
		final List<QueryImageData> data = new ArrayList<QueryImageData>();
		BufferedReader br = null;

		try {
			br = new BufferedReader(new InputStreamReader(is.openStream()));

			String line;
			while ((line = br.readLine()) != null) {
				final QueryImageData qid = QueryImageData.parseCSVLine(line);

				if (qid != null)
					data.add(qid);
			}
		} finally {
			if (br != null)
				br.close();
		}
		return data;
	}
}
