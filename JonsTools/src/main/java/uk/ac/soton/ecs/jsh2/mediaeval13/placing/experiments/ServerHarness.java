package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoAnalysisResult;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoEvaluator;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoPositioningEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;

public class ServerHarness {
	public static void main(String[] args) throws IOException {
		final TLongObjectHashMap<GeoLocation> groundTruth =
				GeoEvaluator.readGroundTruth(ServerHarness.class
						.getResourceAsStream("/uk/ac/soton/ecs/jsh2/mediaeval13/placing/data/validation_latlng"));
		final List<QueryImageData> queries = readQueries(ServerHarness.class
				.getResourceAsStream("/uk/ac/soton/ecs/jsh2/mediaeval13/placing/data/validation.csv"));

		final GeoPositioningEngine engine = new MeanShiftTagEngine(
				new File(args[0]),
				getSkipIds(queries), Integer.parseInt( args[2] ), new
				File(args[1]), Double.parseDouble( args[3] ) );

		final GeoEvaluator eval = new GeoEvaluator(groundTruth, engine, queries);
		final GeoAnalysisResult result = eval.analyse(eval.evaluate());
		System.out.println(result.getDetailReport());
	}

	private static TLongArrayList getSkipIds(List<QueryImageData> queries) {
		final TLongArrayList ids = new TLongArrayList(queries.size());

		for (final QueryImageData q : queries)
			ids.add(q.flickrId);

		return ids;
	}

	static List<QueryImageData> readQueries(InputStream is) throws IOException {
		final List<QueryImageData> data = new ArrayList<QueryImageData>();
		BufferedReader br = null;

		try {
			br = new BufferedReader(new InputStreamReader(is));

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