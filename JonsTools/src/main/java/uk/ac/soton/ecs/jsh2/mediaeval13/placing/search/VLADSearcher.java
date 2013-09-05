package uk.ac.soton.ecs.jsh2.mediaeval13.placing.search;

import gnu.trove.list.array.TLongArrayList;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.openimaj.image.MBFImage;
import org.openimaj.io.IOUtils;
import org.openimaj.util.pair.LongFloatPair;

import uk.ac.soton.ecs.jsh2.mediaeval13.searchengines.LongVLADSearchEngine;

/**
 * VisualSearcher based on VLAD (actually a {@link LongVLADSearchEngine})
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class VLADSearcher implements VisualSearcher {
	TLongArrayList ids;
	LongVLADSearchEngine engine;
	private IndexSearcher meta;
	private RandomAccessFile raf;

	public VLADSearcher(File features, File index, IndexSearcher meta) throws IOException {
		ids = readFeatureIds(features);
		engine = IOUtils.readFromFile(index);

		this.raf = new RandomAccessFile(features, "r");

		this.meta = meta;
	}

	private TLongArrayList readFeatureIds(File features) throws IOException {
		final TLongArrayList allIds = new TLongArrayList(7800000);
		final DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(features)));
		try {
			while (true) {
				allIds.add(dis.readLong());

				int rem = 128 * 4;
				while (rem != 0) {
					final long r = dis.skip(rem);
					if (r > 0)
						rem -= r;
				}
			}
		} catch (final IOException e) {
			dis.close();
		}
		return allIds;
	}

	@Override
	public ScoreDoc[] search(MBFImage query, int numResults) throws IOException {
		return linkResults(engine.search(query, numResults));
	}

	@Override
	public ScoreDoc[] search(long flickrId, int numResults) throws IOException {
		try {
			final float[] vec = readVector(flickrId);

			return linkResults(engine.search(vec, numResults));
		} catch (final IllegalArgumentException iae) {
			if (iae.getMessage().equals("FlickrId not found in features list")) {
				// image was probably missing/had no features/wasn't downloaded
				return new ScoreDoc[0];
			}
			throw iae;
		}
	}

	private synchronized float[] readVector(long flickrId) throws IOException {
		final long idx = ids.binarySearch(flickrId);
		if (idx < 0) {
			throw new IllegalArgumentException("FlickrId not found in features list");
		}

		final long longOffset = idx * (8 + 4 * engine.data.numDimensions());
		raf.seek(longOffset);
		if (raf.readLong() != flickrId) {
			throw new IOException("Wrong ID found");
		}

		final float[] vec = new float[engine.data.numDimensions()];
		for (int i = 0; i < vec.length; i++)
			vec[i] = raf.readFloat();
		return vec;
	}

	private ScoreDoc[] linkResults(List<LongFloatPair> search) throws IOException {
		final ScoreDoc[] docs = new ScoreDoc[search.size()];

		for (int i = 0; i < docs.length; i++) {
			final LongFloatPair r = search.get(i);
			final Query q = NumericRangeQuery.newLongRange("id", r.first, r.first, true, true);
			docs[i] = meta.search(q, 1).scoreDocs[0];
			docs[i].score = r.second;
		}

		return docs;
	}
}
