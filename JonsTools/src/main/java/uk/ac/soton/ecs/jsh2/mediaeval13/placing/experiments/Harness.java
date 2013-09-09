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
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.old.meanshift.BasicCEDDEngine;

@Deprecated
public class Harness {
	public static void main(String[] args) throws IOException {
		final TLongObjectHashMap<GeoLocation> groundTruth =
				GeoEvaluator.readGroundTruth(Harness.class
						.getResourceAsStream("/uk/ac/soton/ecs/jsh2/mediaeval13/placing/data/validation_latlng"));
		final List<QueryImageData> queries = readQueries(Harness.class
				.getResourceAsStream("/uk/ac/soton/ecs/jsh2/mediaeval13/placing/data/validation.csv"));

		// final GeoPositioningEngine engine = new RandomPositioningEngine();
		// final GeoPositioningEngine engine = new
		// PriorRandomBinnedPositioningEngine(new File(
		// "/Volumes/SSD/mediaeval13/placing/training_latlng"),
		// getSkipIds(queries));
		// final GeoPositioningEngine engine = new NaiveBayesFilteredTagEngine(
		// new File("/Volumes/SSD/mediaeval13/placing/places.lucene"),
		// getSkipIds(queries),
		// new File("/Volumes/SSD/mediaeval13/placing/training_latlng"));
		// final GeoPositioningEngine engine = new CachingMeanShiftTagEngine(
		// new File("/Volumes/SSD/mediaeval13/placing/places.lucene"),
		// getSkipIds(queries), 1000, new
		// File("/Volumes/SSD/mediaeval13/placing/training_latlng"), 0.01);
		// final GeoPositioningEngine engine = new ExternalDataEngine(
		// new File("/Users/jsh2/Desktop/positions.txt"));
		// final GeoPositioningEngine engine = new WeightedLSHEngine(
		// new File("/Volumes/SSD/mediaeval13/placing/places.lucene"),
		// new
		// File("/Volumes/SSD/mediaeval13/placing/sift1x-dups/edges-v2.txt"),
		// getSkipIds(queries), 1, 1000, 0.005
		// );
		final GeoPositioningEngine engine = new BasicCEDDEngine(
				new File("/Volumes/SSD/mediaeval13/placing/places.lucene"), new File(
						"/Volumes/SSD/mediaeval13/placing/cedd.bin"),
				getSkipIds(queries), 100, 1000, 0.005
				);

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
