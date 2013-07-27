package uk.ac.soton.ecs.jsh2.mediaeval13;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.LineIterator;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.openimaj.util.function.Operation;
import org.openimaj.util.iterator.IterableIterator;
import org.openimaj.util.parallel.GlobalExecutorPool.DaemonThreadFactory;
import org.openimaj.util.parallel.Parallel;
import org.openimaj.util.parallel.partition.FixedSizeChunkPartitioner;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.shape.Shape;

public class LuceneTests {
	private static final String FIELD_ID = "id";
	private static final String FIELD_USER = "user";
	private static final String FIELD_URL = "url";
	private static final String FIELD_TAKEN = "taken";
	private static final String FIELD_UPLOADED = "uploaded";
	private static final String FIELD_VIEWS = "views";

	final static String CSV_REGEX = ",(?=(?:[^\"]*\"[^\"]*\")*(?![^\"]*\"))";

	public static void main(String[] args) throws IOException {
		final String latlngPath = "/Volumes/SSD/training_latlng";
		final String csvPath = "/Volumes/My Book/mediaeval-placing/all.csv";

		final TLongArrayList ids = new TLongArrayList(7600000);
		final TFloatArrayList lats = new TFloatArrayList(7600000);
		final TFloatArrayList lons = new TFloatArrayList(7600000);

		BufferedReader br = new BufferedReader(new FileReader(latlngPath));
		String line;
		br.readLine(); // skip first line
		while ((line = br.readLine()) != null) {
			final String[] parts = line.split(" ");

			ids.add(Long.parseLong(parts[0]));
			lats.add(Float.parseFloat(parts[1]));
			lons.add(Float.parseFloat(parts[2]));
		}
		br.close();

		final SpatialContext ctx = SpatialContext.GEO;
		final SpatialPrefixTree grid = new GeohashPrefixTree(ctx, 11);
		final RecursivePrefixTreeStrategy strategy = new RecursivePrefixTreeStrategy(grid, "location");

		final StandardAnalyzer a = new StandardAnalyzer(Version.LUCENE_43);
		final IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_43, a);
		iwc.setRAMBufferSizeMB(256);
		Directory directory;
		directory = new SimpleFSDirectory(new File("/Volumes/SSD/lucene-test-index-2"));
		final IndexWriter indexWriter = new IndexWriter(directory, iwc);

		final AtomicInteger counter = new AtomicInteger();
		br = new BufferedReader(new FileReader(csvPath));
		final LineIterator iter = new LineIterator(br);

		final ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(4, new DaemonThreadFactory());
		Parallel.forEachPartitioned(new FixedSizeChunkPartitioner<String>(IterableIterator.in(iter), 1000),
				new Operation<Iterator<String>>() {
					@Override
					public void perform(Iterator<String> it) {
						int bsize = 0;
						while (it.hasNext()) {
							final String line = it.next();
							if (line.startsWith("photo"))
								continue;

							final String[] parts = line.split(CSV_REGEX);
							// photoID,accuracy,userID,photoLink,photoTags,DateTaken,DateUploaded,views,licenseID
							final long flickrId = Long.parseLong(parts[0]);

							final int idx = ids.indexOf(flickrId);
							if (idx < 0)
								continue;

							final String userId = parts[2];
							final String url = parts[3];
							final String tags = parts[4];
							final Date taken = new Date(Long.parseLong(parts[5]));
							final Date uploaded = new Date(Long.parseLong(parts[6]));
							final int views = Integer.parseInt(parts[7]);

							final Document doc = makeDocument(flickrId, userId, url, tags, taken, uploaded, views,
									lats.get(idx),
									lons.get(idx), ctx, strategy);
							try {
								indexWriter.addDocument(doc);
							} catch (final IOException e) {
								e.printStackTrace();
							}

							bsize++;
						}
						synchronized (LuceneTests.class) {
							System.out.println(counter.addAndGet(bsize));
						}
					}
				}, pool);
		indexWriter.commit();
		indexWriter.close();
	}

	@SuppressWarnings("deprecation")
	private static Document makeDocument(long flickrId, String userId, String url, String tags, Date taken,
			Date uploaded, int views, float lat, float lon, SpatialContext ctx, SpatialStrategy strategy)
	{
		final Document doc = new Document();

		doc.add(new LongField(FIELD_ID, flickrId, Store.YES));
		doc.add(new StringField(FIELD_USER, userId, Store.YES));
		doc.add(new StringField(FIELD_URL, url, Store.YES));
		doc.add(new TextField(FIELD_USER, tags, Store.NO));
		doc.add(new StringField(FIELD_TAKEN, DateTools.dateToString(taken, DateTools.Resolution.MINUTE), Field.Store.YES));
		doc.add(new StringField(FIELD_UPLOADED, DateTools.dateToString(uploaded, DateTools.Resolution.MINUTE),
				Field.Store.YES));
		doc.add(new IntField(FIELD_VIEWS, views, Store.YES));

		final Shape point = ctx.makePoint(lon, lat);
		for (final IndexableField f : strategy.createIndexableFields(point)) {
			doc.add(f);
		}
		doc.add(new StoredField(strategy.getFieldName(), ctx.toString(point)));

		return doc;
	}
}
