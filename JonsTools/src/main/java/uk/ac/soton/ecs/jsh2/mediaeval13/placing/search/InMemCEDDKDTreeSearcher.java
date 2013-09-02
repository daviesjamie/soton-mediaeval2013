package uk.ac.soton.ecs.jsh2.mediaeval13.placing.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.openimaj.image.MBFImage;
import org.openimaj.knn.approximate.ByteNearestNeighboursKDTree;
import org.openimaj.util.pair.IntFloatPair;
import org.openimaj.util.pair.LongFloatPair;

public class InMemCEDDKDTreeSearcher extends InMemCEDDSearcher {
	ByteNearestNeighboursKDTree tree;

	public InMemCEDDKDTreeSearcher(String ceddDataFile, IndexSearcher meta) throws IOException {
		super(ceddDataFile, meta);

		this.tree = new ByteNearestNeighboursKDTree(data, 8, 720);
	}

	@Override
	public ScoreDoc[] search(MBFImage query, int numResults) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScoreDoc[] search(long flickrId, int numResults) throws IOException {
		final int idx = findQueryIdx(flickrId);
		final byte[] query = data[idx];

		final List<IntFloatPair> res = tree.searchKNN(query, numResults * 10);
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
