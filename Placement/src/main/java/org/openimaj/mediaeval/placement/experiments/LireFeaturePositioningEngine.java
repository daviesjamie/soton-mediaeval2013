package org.openimaj.mediaeval.placement.experiments;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.ImageSearcher;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Bits;
import org.openimaj.image.FImage;
import org.openimaj.image.pixel.FValuePixel;
import org.openimaj.mediaeval.placement.data.LireFeatures;
import org.openimaj.mediaeval.placement.search.GenericFastImageSearcher;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoPositioningEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;

public class LireFeaturePositioningEngine implements GeoPositioningEngine {

	private IndexReader lireReader;
	private IndexSearcher luceneSearcher;
	private IndexSearcher lireSearcher;
	private TLongArrayList skipIds;
	private FImage prior;
	private LireFeatures feature;
	private int maxHits;

	public LireFeaturePositioningEngine(LireFeatures feature, int maxHits, File luceneIndex, File lireIndex,
			TLongArrayList skipIds) throws IOException {
		final Directory luceneDirectory = new SimpleFSDirectory(luceneIndex);
		final Directory lireDirectory = new SimpleFSDirectory(lireIndex);

		final IndexReader luceneReader = DirectoryReader.open(luceneDirectory);
		this.lireReader = DirectoryReader.open(lireDirectory);
		this.luceneSearcher = new IndexSearcher(luceneReader);
		this.lireSearcher = new IndexSearcher(lireReader);

		this.skipIds = skipIds;
		this.feature = feature;
		this.maxHits = maxHits;

		this.prior = createPrior(luceneReader);
	}

	private FImage createPrior(IndexReader reader) throws IOException {
		final Bits liveDocs = MultiFields.getLiveDocs(reader);
		final Set<String> fields = new HashSet<String>();
		fields.add(LuceneIndexBuilder.FIELD_LOCATION);

		final FImage img = new FImage(360, 180);
		for (int i = 0; i < reader.maxDoc(); i++) {
			if (liveDocs != null && !liveDocs.get(i))
				continue;

			final Document doc = reader.document(i, fields);
			final String[] llstr = doc.get(LuceneIndexBuilder.FIELD_LOCATION).split(" ");
			final float x = Float.parseFloat(llstr[0]) + 180;
			final float y = 90 - Float.parseFloat(llstr[1]);

			img.pixels[(int) (y % img.height)][(int) (x % img.width)] = 1;

			if (i % 10000 == 0)
				System.out.println(i);
		}

		img.divideInplace(img.sum());

		return img;
	}

	private FImage search(long photoId) {
		try {
			final Query query = NumericRangeQuery.newLongRange(LuceneIndexBuilder.FIELD_ID, photoId, photoId, true,
					true);
			int docId = lireSearcher.search(query, 1).scoreDocs[0].doc;
			Document doc = lireSearcher.doc(docId);

			ImageSearcher imageSearcher = new GenericFastImageSearcher(maxHits, feature.fclass, feature.name);
			ImageSearchHits hits = imageSearcher.search(doc, lireReader);

			final FImage img = new FImage(360, 180);
			img.fill(1f / (img.height * img.width));

			for (int i = 0; i < hits.length(); i++) {
				final long pid = Long.parseLong(hits.doc(i).get(LuceneIndexBuilder.FIELD_ID));
				if (skipIds.contains(pid))
					continue;

				final Query q = NumericRangeQuery.newLongRange(LuceneIndexBuilder.FIELD_ID, pid, pid, true, true);
				final int did = luceneSearcher.search(q, 1).scoreDocs[0].doc;
				final Document d = luceneSearcher.doc(did);

				final String[] llstr = d.get(LuceneIndexBuilder.FIELD_LOCATION).split(" ");
				final float x = Float.parseFloat(llstr[0]) + 180;
				final float y = 90 - Float.parseFloat(llstr[1]);

				img.pixels[(int) (y % img.height)][(int) (x % img.width)] = 1;
			}

			img.divideInplace(img.sum());

			return img;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public GeoLocationEstimate estimateLocation(QueryImageData query) {
		final FImage map = search(query.flickrId);
		map.multiplyInplace(prior);

		final FValuePixel pos = map.maxPixel();
		return new GeoLocationEstimate(90 - pos.y, pos.x - 180, 100 * pos.value);
	}

}
