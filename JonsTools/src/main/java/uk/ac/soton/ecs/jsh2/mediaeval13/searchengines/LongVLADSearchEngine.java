package uk.ac.soton.ecs.jsh2.mediaeval13.searchengines;

import java.util.ArrayList;
import java.util.List;

import org.openimaj.image.MBFImage;
import org.openimaj.image.indexing.vlad.VLADIndexerData;
import org.openimaj.knn.pq.FloatADCNearestNeighbours;
import org.openimaj.util.pair.IntFloatPair;
import org.openimaj.util.pair.LongFloatPair;

/**
 * Search engine for VLAD features based on a FloatADCNearestNeighbours. Assumes
 * image ids are longs.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class LongVLADSearchEngine {
	public VLADIndexerData data;
	public FloatADCNearestNeighbours nn;
	public long[] ids;

	public LongVLADSearchEngine(long[] ids, VLADIndexerData data, byte[][] pqData) {
		nn = new FloatADCNearestNeighbours(data.getProductQuantiser(), pqData, data.numDimensions());
		this.ids = ids;
		this.data = data;
	}

	public List<LongFloatPair> search(MBFImage query, int numResults) {
		final List<IntFloatPair> res = nn.searchKNN(data.extractPcaVlad(query), numResults);

		return sanitise(res);
	}

	public List<LongFloatPair> search(float[] pcaVlad, int numResults) {
		final List<IntFloatPair> res = nn.searchKNN(pcaVlad, numResults);

		return sanitise(res);
	}

	private List<LongFloatPair> sanitise(final List<IntFloatPair> res) {
		final List<LongFloatPair> results = new ArrayList<LongFloatPair>(res.size());
		for (final IntFloatPair r : res) {
			results.add(LongFloatPair.pair(ids[r.first], r.second));
		}
		return results;
	}
}
