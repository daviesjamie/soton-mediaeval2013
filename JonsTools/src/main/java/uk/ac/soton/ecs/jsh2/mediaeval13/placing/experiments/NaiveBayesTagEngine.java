package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.Version;
import org.openimaj.image.FImage;
import org.openimaj.image.pixel.FValuePixel;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoPositioningEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;

public class NaiveBayesTagEngine implements GeoPositioningEngine {
	private IndexSearcher searcher;
	private TLongArrayList skipIds;
	private FImage prior;

	public NaiveBayesTagEngine(File file, TLongArrayList skipIds) throws IOException {
		final Directory directory = new SimpleFSDirectory(file);
		final IndexReader reader = DirectoryReader.open(directory);
		this.searcher = new IndexSearcher(reader);
		this.skipIds = skipIds;

		this.prior = createPrior(reader);
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

	FImage search(String query) {
		try {
			// final TermQuery q = new TermQuery(new
			// Term(LuceneIndexBuilder.FIELD_TAGS, query));
			final Query q = new QueryParser(Version.LUCENE_43, "tags", new StandardAnalyzer(Version.LUCENE_43))
					.parse(query);
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

				img.pixels[(int) (y % img.height)][(int) (x % img.width)] = 1;
			}

			img.divideInplace(img.sum());

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
			map.multiplyInplace(search(queryTerms[i]));
		}

		map.multiplyInplace(prior);

		final FValuePixel pos = prior.maxPixel();
		return new GeoLocationEstimate(90 - pos.y, pos.x - 180, 100 * pos.value);
	}
}
