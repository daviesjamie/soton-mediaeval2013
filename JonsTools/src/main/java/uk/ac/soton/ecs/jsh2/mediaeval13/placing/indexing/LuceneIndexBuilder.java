package uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.LineIterator;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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

/**
 * Build a lucene index from all the metadata of all the geotagged (i.e.
 * training) images
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class LuceneIndexBuilder {
	public static final String FIELD_ID = "id";
	public static final String FIELD_USER = "user";
	public static final String FIELD_URL = "url";
	public static final String FIELD_TAKEN = "taken";
	public static final String FIELD_UPLOADED = "uploaded";
	public static final String FIELD_VIEWS = "views";
	public static final String FIELD_TAGS = "tags";
	public static final String FIELD_LOCATION = "location";

	private final static String CSV_REGEX = ",(?=(?:[^\"]*\"[^\"]*\")*(?![^\"]*\"))";

	public static void main(String[] args) throws IOException {
		final String latlngPath = "/Volumes/SSD/mediaeval13/placing/training_latlng";
		final String csvPath = "/Volumes/SSD/mediaeval13/placing/all.csv";
		final String indexPath = "/Volumes/SSD/mediaeval13/placing/places.lucene.v2";

		buildIndex(latlngPath, csvPath, indexPath);
	}

	private static void buildIndex(final String latlngPath, final String csvPath, final String indexPath)
			throws FileNotFoundException,
			IOException
	{
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
		final RecursivePrefixTreeStrategy strategy = new RecursivePrefixTreeStrategy(grid, FIELD_LOCATION);

		final StandardAnalyzer a = new StandardAnalyzer(Version.LUCENE_43);
		final IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_43, a);
		iwc.setRAMBufferSizeMB(512);
		Directory directory;
		directory = new SimpleFSDirectory(new File(indexPath));
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
							final long taken = Long.parseLong(parts[5]);
							final long uploaded = Long.parseLong(parts[6]);
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
						synchronized (LuceneIndexBuilder.class) {
							System.out.println(counter.addAndGet(bsize));
						}
					}
				}, pool);
		indexWriter.commit();
		indexWriter.close();
	}

	@SuppressWarnings("deprecation")
	private static Document makeDocument(long flickrId, String userId, String url, String tags, long taken,
			long uploaded, int views, float lat, float lon, SpatialContext ctx, SpatialStrategy strategy)
	{
		final Document doc = new Document();

		doc.add(new LongField(FIELD_ID, flickrId, Store.YES));
		doc.add(new StringField(FIELD_USER, userId, Store.YES));
		doc.add(new StringField(FIELD_URL, url, Store.YES));
		doc.add(new TextField(FIELD_TAGS, tags, Store.YES));
		doc.add(new LongField(FIELD_TAKEN, taken, Field.Store.YES));
		doc.add(new LongField(FIELD_UPLOADED, uploaded, Field.Store.YES));
		doc.add(new IntField(FIELD_VIEWS, views, Store.YES));

		final Shape point = ctx.makePoint(lon, lat);
		for (final IndexableField f : strategy.createIndexableFields(point)) {
			doc.add(f);
		}
		doc.add(new StoredField(strategy.getFieldName(), ctx.toString(point)));

		return doc;
	}
}
