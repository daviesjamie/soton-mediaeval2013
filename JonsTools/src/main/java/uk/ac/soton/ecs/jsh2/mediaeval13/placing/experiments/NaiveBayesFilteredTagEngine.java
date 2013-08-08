package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments;

import gnu.trove.list.array.TLongArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.openimaj.image.FImage;
import org.openimaj.image.pixel.FValuePixel;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoPositioningEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;

public class NaiveBayesFilteredTagEngine implements GeoPositioningEngine {
	protected IndexSearcher searcher;
	protected TLongArrayList skipIds;
	protected FImage prior;
	private ArrayList<String> filtered;

	public NaiveBayesFilteredTagEngine(File file, TLongArrayList skipIds, File latlngFile) throws IOException {
		final Directory directory = new MMapDirectory(file);
		final IndexReader reader = DirectoryReader.open(directory);
		this.searcher = new IndexSearcher(reader);
		this.skipIds = skipIds;

		// frequencyFilter(reader);
		distanceFilter(reader);

		this.prior = createPrior(latlngFile);
	}

	private void frequencyFilter(final IndexReader reader) throws IOException {
		final Fields fields = MultiFields.getFields(reader);
		final Terms terms = fields.terms("tags");
		final TermsEnum iterator = terms.iterator(null);
		BytesRef byteRef = null;
		this.filtered = new ArrayList<String>();
		while ((byteRef = iterator.next()) != null) {
			if (iterator.docFreq() > 50)
				filtered.add(byteRef.utf8ToString());
		}
	}

	private void distanceFilter(final IndexReader reader) throws IOException {
		frequencyFilter(reader);

		final List<String> toRemove = new ArrayList<String>();
		for (final String term : filtered) {
			System.out.println(term);

			final List<GeoLocation> locs = new ArrayList<GeoLocation>();
			final TermQuery q = new TermQuery(new Term(LuceneIndexBuilder.FIELD_TAGS, term));

			final TopScoreDocCollector collector = TopScoreDocCollector.create(1000000, true);
			searcher.search(q, collector);
			final ScoreDoc[] hits = collector.topDocs().scoreDocs;

			double mlat = 0;
			double mlng = 0;
			for (int i = 0; i < hits.length; ++i) {
				final int docId = hits[i].doc;
				final Document d = searcher.doc(docId);

				final long flickrId = Long.parseLong(d.get(LuceneIndexBuilder.FIELD_ID));
				if (skipIds.contains(flickrId))
					continue;

				final String[] llstr = d.get(LuceneIndexBuilder.FIELD_LOCATION).split(" ");
				final float lng = Float.parseFloat(llstr[0]);
				final float lat = Float.parseFloat(llstr[1]);

				locs.add(new GeoLocation(lat, lng));
				mlat += lat;
				mlng += lng;
			}

			mlat /= locs.size();
			mlng /= locs.size();

			double mdist = 0;
			final GeoLocation mean = new GeoLocation(mlat, mlng);
			for (final GeoLocation g : locs) {
				mdist += g.haversine(mean);
			}
			mdist /= locs.size();

			if (mdist >= 200)
				toRemove.add(term);
		}

		filtered.removeAll(toRemove);
	}

	private FImage createPrior(File latlngFile) throws IOException {
		final BufferedReader br = new BufferedReader(new FileReader(latlngFile));

		final FImage img = new FImage(360, 180);
		img.fill(1f / (img.height * img.width));
		String line;
		br.readLine();
		while ((line = br.readLine()) != null) {
			final String[] parts = line.split(" ");

			if (this.skipIds.contains(Long.parseLong(parts[0])))
				continue;

			final float x = Float.parseFloat(parts[2]) + 180;
			final float y = 90 - Float.parseFloat(parts[1]);

			img.pixels[(int) (y % img.height)][(int) (x % img.width)]++;
		}
		br.close();

		logNorm(img);

		return img;
	}

	protected void logNorm(final FImage img) {
		final double norm = img.sum();
		for (int y = 0; y < img.height; y++)
			for (int x = 0; x < img.width; x++)
				img.pixels[y][x] = (float) Math.log(img.pixels[y][x] / norm);
	}

	protected FImage search(String query) {
		try {
			if (!filtered.contains(query))
				return new FImage(360, 180);

			final TermQuery q = new TermQuery(new Term(LuceneIndexBuilder.FIELD_TAGS, query));

			final TopScoreDocCollector collector = TopScoreDocCollector.create(1000000, true);
			searcher.search(q, collector);
			final ScoreDoc[] hits = collector.topDocs().scoreDocs;

			final FImage img = new FImage(360, 180);
			img.fill(1f / (img.height * img.width));
			for (int i = 0; i < hits.length; ++i) {
				final int docId = hits[i].doc;
				final Document d = searcher.doc(docId);

				final long flickrId = Long.parseLong(d.get(LuceneIndexBuilder.FIELD_ID));
				if (skipIds.contains(flickrId))
					continue;

				final String[] llstr = d.get(LuceneIndexBuilder.FIELD_LOCATION).split(" ");
				final float x = Float.parseFloat(llstr[0]) + 180;
				final float y = 90 - Float.parseFloat(llstr[1]);

				img.pixels[(int) (y % img.height)][(int) (x % img.width)]++;
			}

			logNorm(img);

			return img;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public GeoLocationEstimate estimateLocation(QueryImageData query) {
		final String[] queryTerms = query.tags.split(" ");

		if (queryTerms == null || queryTerms.length == 0) {
			final FValuePixel pos = prior.maxPixel();

			return new GeoLocationEstimate(90 - pos.y, pos.x - 180, 40000);
		}

		final FImage map = search(queryTerms[0]);
		for (int i = 1; i < queryTerms.length; i++) {
			map.addInplace(search(queryTerms[i]));
		}
		map.addInplace(prior);

		final FValuePixel pos = map.maxPixel();
		return new GeoLocationEstimate(90 - pos.y, pos.x - 180, 100 * pos.value);
	}
}
