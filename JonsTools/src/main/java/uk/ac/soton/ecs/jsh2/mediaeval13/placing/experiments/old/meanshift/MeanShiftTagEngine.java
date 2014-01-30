package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.old.meanshift;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.openimaj.image.MBFImage;
import org.openimaj.util.pair.IndependentPair;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocationEstimate;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoPositioningEngine;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.QueryImageData;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.util.Utils;
import uk.ac.soton.ecs.jsh2.mediaeval13.utils.QuickMeanShift;
import uk.ac.soton.ecs.jsh2.mediaeval13.utils.QuickMeanShift.FlatWindow;

public class MeanShiftTagEngine implements GeoPositioningEngine {
	private static final int MIN_BIN_FREQ = 1;
	private static final int MAX_ITERATIONS = 300;

	protected IndexSearcher searcher;
	protected TLongArrayList skipIds;
	protected List<GeoLocation> prior;
	protected int sampleCount;
	protected double bandwidth;

	public MeanShiftTagEngine(File file, TLongArrayList skipIds, int sampleCount, File latlngFile, double bandwidth)
			throws IOException
	{
		final Directory directory = new MMapDirectory(file);
		final IndexReader reader = DirectoryReader.open(directory);
		this.searcher = new IndexSearcher(reader);
		this.skipIds = skipIds;
		this.sampleCount = sampleCount;
		this.bandwidth = bandwidth;

		if (latlngFile != null) {
			prior = Utils.readLatLng(latlngFile, skipIds);
			Collections.shuffle(prior);
			prior = prior.subList(0, sampleCount);
		}
	}

	protected List<GeoLocation> search(String query) {
		try {
			final TermQuery q = new TermQuery(new Term(LuceneIndexBuilder.FIELD_TAGS, query));

			final TopScoreDocCollector collector = TopScoreDocCollector.create(1000000, true);
			searcher.search(q, collector);
			final ScoreDoc[] hits = collector.topDocs().scoreDocs;

			List<GeoLocation> locations = new ArrayList<GeoLocation>(hits.length);
			for (int i = 0; i < hits.length; ++i) {
				final int docId = hits[i].doc;
				final Document d = searcher.doc(docId);

				final long flickrId = Long.parseLong(d.get(LuceneIndexBuilder.FIELD_ID));
				if (skipIds.contains(flickrId))
					continue;

				final String[] llstr = d.get(LuceneIndexBuilder.FIELD_LOCATION).split(" ");
				final float lon = Float.parseFloat(llstr[0]);
				final float lat = Float.parseFloat(llstr[1]);

				locations.add(new GeoLocation(lat, lon));
			}

			if (locations.size() == 0)
				return locations;

			if (locations.size() > sampleCount) {
				Collections.shuffle(locations);
				locations = locations.subList(0, sampleCount);
			} else {
				final int size = locations.size();
				while (locations.size() < sampleCount) {
					locations.add(locations.get((int) (Math.random() * size)));
				}
			}

			return locations;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	// private void writeQueryResult(QueryImageData query, List<GeoLocation>
	// pts) {
	// try {
	// final File file = new File("/Volumes/SSD/mediaeval13/placing/expt-geos-"
	// + sampleCount + "-"
	// + (prior != null ? "prior" : "") + "/" + query.flickrId + ".txt");
	// file.getParentFile().mkdirs();
	//
	// final BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	// for (final GeoLocation l : pts) {
	// bw.write(l.longitude + " " + l.latitude + "\n");
	// }
	// bw.close();
	// } catch (final Exception e) {
	// e.printStackTrace();
	// }
	// }

	@Override
	public GeoLocationEstimate estimateLocation(QueryImageData query) {
		final String[] queryTerms = query.tags.split(" ");

		final List<GeoLocation> pts = search(queryTerms[0]);
		for (int i = 1; i < queryTerms.length; i++) {
			pts.addAll(search(queryTerms[i]));
		}
		if (prior != null) {
			pts.addAll(prior);
		}

		return computeLocationMeanShift(pts);
	}

	protected GeoLocationEstimate computeLocationMeanShift(final List<GeoLocation> pts) {
		final double[][] data = toArray(pts);

		final double[][] seeds = QuickMeanShift.bin_points(data, bandwidth / 2, MIN_BIN_FREQ);
		final IndependentPair<double[][], int[]> result = QuickMeanShift.meanShift(data, bandwidth, seeds,
				FlatWindow.INSTANCE, MAX_ITERATIONS);

		final TIntIntHashMap counts = new TIntIntHashMap();
		for (final int i : result.secondObject())
			counts.adjustOrPutValue(i, 1, 1);

		int maxIdx = -1;
		int maxValue = 0;
		for (final int idx : counts.keys()) {
			final int count = counts.get(idx);
			if (count > maxValue) {
				maxValue = count;
				maxIdx = idx;
			}
		}

		double maxX = -1000;
		double minX = 1000;
		double maxY = -1000;
		double minY = 1000;
		final double[][] biggest = new double[counts.get(maxIdx)][];
		for (int i = 0, j = 0; i < data.length; i++) {
			if (result.secondObject()[i] == maxIdx) {
				biggest[j++] = data[i];

				if (data[i][0] > maxX)
					maxX = data[i][0];
				if (data[i][0] < minX)
					minX = data[i][0];
				if (data[i][1] > maxY)
					maxY = data[i][1];
				if (data[i][1] < minY)
					minY = data[i][1];
			}
		}

		final double dx = maxX - minX;
		final double dy = maxY - minY;
		final double dist = Math.sqrt((dx * dx) + (dy * dy));

		return new GeoLocationEstimate(result.firstObject()[maxIdx][1], result.firstObject()[maxIdx][0], dist);
	}

	private double[][] toArray(List<GeoLocation> pts) {
		final double[][] data = new double[pts.size()][];

		for (int i = 0; i < data.length; i++)
			data[i] = new double[] { pts.get(i).longitude, pts.get(i).latitude };

		return data;
	}

	@Override
	public GeoLocationEstimate estimateLocation(MBFImage image, String[] tags) {
		throw new UnsupportedOperationException();
	}
}
