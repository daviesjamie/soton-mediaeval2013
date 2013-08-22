package uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongObjectProcedure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openimaj.experiment.evaluation.Evaluator;
import org.openimaj.util.function.Operation;
import org.openimaj.util.pair.DoubleDoublePair;
import org.openimaj.util.parallel.Parallel;

/**
 * Implementation of an {@link Evaluator} for testing predicted geolocations
 * against actual ones.
 * <p>
 * Includes a main method for performing testing by reading data from two files
 * - one containing the ground truth in the form: <code>id lat lng</code> and
 * the other containing the predictions in the form:
 * <code>id;lat;lng;estimatedErrorInKM</code>.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class GeoEvaluator implements Evaluator<TLongObjectHashMap<GeoLocationEstimate>, GeoAnalysisResult> {
	TLongObjectHashMap<GeoLocation> groundTruth;
	GeoPositioningEngine engine;
	Collection<QueryImageData> queries;

	public GeoEvaluator(TLongObjectHashMap<GeoLocation> groundTruth, GeoPositioningEngine engine,
			Collection<QueryImageData> queries)
	{
		this.queries = queries;
		this.engine = engine;
		this.groundTruth = groundTruth;
	}

	public GeoEvaluator(TLongObjectHashMap<GeoLocation> gt)
	{
		this.groundTruth = gt;
	}

	@Override
	public TLongObjectHashMap<GeoLocationEstimate> evaluate() {
		final TLongObjectHashMap<GeoLocationEstimate> results = new TLongObjectHashMap<GeoLocationEstimate>();

		// for (final QueryImageData q : queries) {
		// results.put(q.flickrId, engine.estimateLocation(q));
		// }

		Parallel.forEach(queries, new Operation<QueryImageData>() {
			@Override
			public void perform(QueryImageData q) {
				if (q.flickrId == 179752334)
					System.out.println("here");
				final GeoLocationEstimate location = engine.estimateLocation(q);
				synchronized (results) {
					results.put(q.flickrId, location);
					System.out.println(results.size());
				}
			}
		});

		return results;
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

	public static void main(String[] args) throws IOException {
		final TLongObjectHashMap<GeoLocation> gt = readGroundTruth(args[0]);
		final TLongObjectHashMap<GeoLocationEstimate> predictions = readPredictions(args[1]);

		final GeoEvaluator eval = new GeoEvaluator(gt);
		final GeoAnalysisResult result = eval.analyse(predictions);
		System.out.println();
		System.out.println("Ground truth file: " + args[0]);
		System.out.println(" Predictions file: " + args[1]);
		System.out.println();
		System.out.println(result.getDetailReport());
		System.out.println();
	}

	public static TLongObjectHashMap<GeoLocation> readGroundTruth(String filename) throws IOException {
		FileInputStream fis = null; // new FileInputStream(filename);
		try {
			fis = new FileInputStream(new File(filename));
			return readGroundTruth(fis);
		} finally {
			if (fis != null)
				fis.close();
		}
	}

	public static TLongObjectHashMap<GeoLocation> readGroundTruth(InputStream is) throws IOException {
		final TLongObjectHashMap<GeoLocation> data = new TLongObjectHashMap<GeoLocation>();

		final BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = br.readLine()) != null) {
			try {
				final String[] parts = line.split(" ");
				final long id = Long.parseLong(parts[0]);
				final double lat = Double.parseDouble(parts[1]);
				final double lng = Double.parseDouble(parts[2]);

				data.put(id, new GeoLocation(lat, lng));
			} catch (final NumberFormatException nfe) {
				// ignore line
			}
		}

		return data;
	}

	private static TLongObjectHashMap<GeoLocationEstimate> readPredictions(String filename) throws IOException {
		final TLongObjectHashMap<GeoLocationEstimate> data = new TLongObjectHashMap<GeoLocationEstimate>();
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				try {
					final String[] parts = line.split(";");
					final long id = Long.parseLong(parts[0]);
					final double lat = Double.parseDouble(parts[1]);
					final double lng = Double.parseDouble(parts[2]);
					final double estimate = Double.parseDouble(parts[3]);

					data.put(id, new GeoLocationEstimate(lat, lng, estimate));
				} catch (final NumberFormatException nfe) {
					// ignore line
				}
			}
		} finally {
			if (br != null)
				br.close();
		}

		return data;
	}
}
