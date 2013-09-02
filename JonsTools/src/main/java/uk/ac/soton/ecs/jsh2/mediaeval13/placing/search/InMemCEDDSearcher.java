package uk.ac.soton.ecs.jsh2.mediaeval13.placing.search;

import gnu.trove.list.array.TLongArrayList;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.openimaj.image.MBFImage;
import org.openimaj.util.pair.LongFloatPair;
import org.openimaj.util.queue.BoundedPriorityQueue;

public class InMemCEDDSearcher implements VisualSearcher {
	long[] ids;
	byte[][] data;
	private IndexSearcher meta;

	public InMemCEDDSearcher(String ceddDataFile, IndexSearcher meta) throws IOException {
		this.meta = meta;

		final DataInputStream dis = new DataInputStream(new FileInputStream(ceddDataFile));

		final TLongArrayList tmpIds = new TLongArrayList();
		final List<byte[]> tmpData = new ArrayList<byte[]>();
		try {
			while (true) {
				final long id = dis.readLong();
				final byte[] d = new byte[144];
				dis.readFully(d);

				tmpIds.add(id);
				tmpData.add(d);
			}
		} catch (final EOFException eof) {
			dis.close();
		}

		this.ids = tmpIds.toArray();
		this.data = tmpData.toArray(new byte[tmpData.size()][]);
	}

	@Override
	public ScoreDoc[] search(MBFImage query, int numResults) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScoreDoc[] search(long flickrId, int numResults) throws IOException {
		final int idx = findQueryIdx(flickrId);
		final byte[] query = data[idx];

		final BoundedPriorityQueue<LongFloatPair> results = new BoundedPriorityQueue<LongFloatPair>(numResults,
				LongFloatPair.SECOND_ITEM_ASCENDING_COMPARATOR);

		LongFloatPair tmp = new LongFloatPair();
		for (int i = 0; i < numResults; i++)
			results.add(new LongFloatPair(-1, Float.MAX_VALUE));

		for (int i = 0; i < ids.length; i++) {
			final float score = this.getDistance(query, data[i]);
			tmp.first = ids[i];
			tmp.second = score;
			tmp = results.offerItem(tmp);
		}

		final ScoreDoc[] finalres = linkResults(results.toOrderedListDestructive());
		return finalres;
	}

	private int findQueryIdx(long flickrId) {
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == flickrId)
				return i;
		}

		return -1;
	}

	protected ScoreDoc[] linkResults(List<LongFloatPair> search) throws IOException {
		final List<ScoreDoc> docs = new ArrayList<ScoreDoc>(search.size());

		for (int i = 0; i < search.size(); i++) {
			final LongFloatPair r = search.get(i);
			final Query q = NumericRangeQuery.newLongRange("id", r.first, r.first, true, true);
			final TopDocs rr = meta.search(q, 1);
			if (rr.scoreDocs.length >= 1) {
				docs.add(rr.scoreDocs[0]);
				rr.scoreDocs[0].score = r.second;
			}
		}

		return docs.toArray(new ScoreDoc[docs.size()]);
	}

	public float getDistance(byte[] f1, byte[] f2) { // added by mlux
		// Init Tanimoto coefficient
		double Result = 0;
		double Temp1 = 0;
		double Temp2 = 0;
		double TempCount1 = 0;
		double TempCount2 = 0;
		double TempCount3 = 0;

		for (int i = 0; i < f1.length; i++) {
			Temp1 += f1[i];
			Temp2 += f2[i];
		}

		if (Temp1 == 0 || Temp2 == 0)
			return 100f;
		if (Temp1 == 0 && Temp2 == 0)
			return 0f;

		if (Temp1 > 0 && Temp2 > 0) {
			for (int i = 0; i < f1.length; i++) {
				final double iTmp1 = f1[i] / Temp1;
				final double iTmp2 = f2[i] / Temp2;
				TempCount1 += iTmp1 * iTmp2;
				TempCount2 += iTmp2 * iTmp2;
				TempCount3 += iTmp1 * iTmp1;

			}

			Result = (100 - 100 * (TempCount1 / (TempCount2 + TempCount3
					- TempCount1))); // Tanimoto
		}
		return (float) Result;

	}
}
