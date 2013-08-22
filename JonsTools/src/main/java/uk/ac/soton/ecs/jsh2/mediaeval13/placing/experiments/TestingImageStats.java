package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments;

import java.io.IOException;
import java.util.List;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;
import cern.colt.Arrays;

public class TestingImageStats {
	public static void main(String[] args) throws IOException {
		final List<QueryImageData> queries = Harness.readQueries(Harness.class
				.getResourceAsStream("/uk/ac/soton/ecs/jsh2/mediaeval13/placing/data/validation.csv"));

		int count = 0;
		for (final QueryImageData d : queries) {
			count = Math.max(count, d.tags.trim().split(" ").length);
		}

		final int[] hist = new int[count + 1];
		for (final QueryImageData d : queries) {
			hist[d.tags.trim().split(" ").length]++;
		}

		System.out.println(Arrays.toString(hist));
	}
}
