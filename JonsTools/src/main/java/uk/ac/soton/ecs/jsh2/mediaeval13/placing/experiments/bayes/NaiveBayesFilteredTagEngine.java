package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.bayes;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
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
import org.apache.lucene.util.BytesRef;
import org.openimaj.image.FImage;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;

/**
 * Naive bayes on the tags, with some filtering based on geographic spread
 * and/or occurance counts
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class NaiveBayesFilteredTagEngine extends NaiveBayesTagEngine {
	protected IndexSearcher searcher;
	protected TLongArrayList skipIds;
	protected FImage prior;
	private ArrayList<String> filtered;

	/**
	 * Construct with the given data. If the latlngFile is null, then the prior
	 * will be uniform.
	 * 
	 * @param file
	 * @param skipIds
	 * @param latlngFile
	 * @throws IOException
	 */
	public NaiveBayesFilteredTagEngine(File file, TLongArrayList skipIds, File latlngFile, int freq, double dist)
			throws IOException
	{
		super(file, skipIds, latlngFile);

		if (freq > 0)
			frequencyFilter(searcher.getIndexReader(), freq);
		if (dist > 0)
			distanceFilter(searcher.getIndexReader(), dist);
	}

	private void frequencyFilter(final IndexReader reader, int freq) throws IOException {
		final Fields fields = MultiFields.getFields(reader);
		final Terms terms = fields.terms("tags");
		final TermsEnum iterator = terms.iterator(null);
		BytesRef byteRef = null;
		this.filtered = new ArrayList<String>();
		while ((byteRef = iterator.next()) != null) {
			if (iterator.docFreq() > freq)
				filtered.add(byteRef.utf8ToString());
		}
	}

	private void distanceFilter(final IndexReader reader, double dist) throws IOException {
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

			if (mdist >= dist)
				toRemove.add(term);
		}

		filtered.removeAll(toRemove);
	}

	@Override
	protected FImage search(String query) {
		if (!filtered.contains(query))
			return new FImage(360, 180);

		return super.search(query);
	}
}
