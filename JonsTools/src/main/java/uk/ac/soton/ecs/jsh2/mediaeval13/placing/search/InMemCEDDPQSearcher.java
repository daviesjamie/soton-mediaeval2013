package uk.ac.soton.ecs.jsh2.mediaeval13.placing.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.openimaj.data.RandomData;
import org.openimaj.image.MBFImage;
import org.openimaj.io.IOUtils;
import org.openimaj.knn.pq.ByteADCNearestNeighbours;
import org.openimaj.knn.pq.ByteProductQuantiser;
import org.openimaj.knn.pq.ByteProductQuantiserUtilities;
import org.openimaj.util.pair.IntFloatPair;
import org.openimaj.util.pair.LongFloatPair;

public class InMemCEDDPQSearcher extends InMemCEDDSearcher {
	ByteADCNearestNeighbours nn;

	public InMemCEDDPQSearcher(File ceddDataFile, IndexSearcher meta) throws IOException {
		super(ceddDataFile, meta);

		final File adcnn = new File(ceddDataFile.getAbsolutePath().replace(".bin", "-adcnn.bin"));
		if (!adcnn.exists()) {
			System.out.println("getting samples");
			final byte[][] sample = new byte[10000][];
			final int[] sampIds = RandomData.getUniqueRandomInts(sample.length, 0, this.data.length);
			for (int i = 0; i < sample.length; i++) {
				sample[i] = data[sampIds[i]];
			}

			System.out.println("Building PQ");
			final ByteProductQuantiser pq = ByteProductQuantiserUtilities.train(sample, 18, 50);

			System.out.println("Building NN");
			this.nn = new ByteADCNearestNeighbours(pq, this.data);
			System.out.println("done");

			adcnn.getParentFile().mkdirs();
			IOUtils.writeToFile(nn, adcnn);
		} else {
			nn = IOUtils.readFromFile(adcnn);
		}
	}

	@Override
	public ScoreDoc[] search(MBFImage query, int numResults) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScoreDoc[] search(long flickrId, int numResults) throws IOException {
		final int idx = findQueryIdx(flickrId);
		final byte[] query = data[idx];

		final List<IntFloatPair> res = nn.searchKNN(query, numResults);
		List<LongFloatPair> results = new ArrayList<LongFloatPair>(res.size());
		for (int i = 0; i < res.size(); i++) {
			final IntFloatPair r = res.get(i);
			results.add(LongFloatPair.pair(this.ids[r.first], getDistance(query, this.data[r.first])));
		}

		Collections.sort(results, LongFloatPair.SECOND_ITEM_ASCENDING_COMPARATOR);
		results = results.subList(0, numResults);

		return linkResults(results);
	}

	private int findQueryIdx(long flickrId) {
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == flickrId)
				return i;
		}

		return -1;
	}
}
