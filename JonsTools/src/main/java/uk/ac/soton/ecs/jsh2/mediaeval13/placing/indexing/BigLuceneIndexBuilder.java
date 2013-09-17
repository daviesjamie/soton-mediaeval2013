package uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing;

import gnu.trove.list.array.TLongArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
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
public class BigLuceneIndexBuilder {

    public static final String FIELD_ID = "id";
    public static final String FIELD_USER = "user";
    public static final String FIELD_URL = "url";
    public static final String FIELD_TAKEN = "taken";
    public static final String FIELD_UPLOADED = "uploaded";
    public static final String FIELD_VIEWS = "views";
    public static final String FIELD_TAGS = "tags";
    public static final String FIELD_LOCATION = "location";
    
    private static final String CSV_REGEX = ", (?=(?:[^\"]*\"[^\"]*\")*(?![^\"]*\"))";

    public static void main( String[] args ) throws IOException {
        final String csvPath = args[0];
        final String indexPath = args[1];
        final String testSetPath = args[2];

        buildIndex( csvPath, indexPath, testSetPath );
    }

    private static void buildIndex( final String csvPath, final String indexPath, final String testSetPath ) throws FileNotFoundException, IOException {
        final TLongArrayList excludeIds = new TLongArrayList( 250000 );
        final ArrayList<String> excludeUsers = new ArrayList<String>( 75000 );

        BufferedReader br = new BufferedReader( new FileReader( testSetPath ) );
        String line;
        while( (line = br.readLine()) != null ) {
            final String[] parts = line.split( "," );

            excludeIds.add( Long.parseLong( parts[0] ) );
            excludeUsers.add( parts[2] );
        }
        br.close();

        final SpatialContext ctx = SpatialContext.GEO;
        final SpatialPrefixTree grid = new GeohashPrefixTree( ctx, 11 );
        final RecursivePrefixTreeStrategy strategy = new RecursivePrefixTreeStrategy( grid, FIELD_LOCATION );

        final StandardAnalyzer a = new StandardAnalyzer( Version.LUCENE_43 );
        final IndexWriterConfig iwc = new IndexWriterConfig( Version.LUCENE_43, a );
        iwc.setRAMBufferSizeMB( 512 );
        Directory directory;
        directory = new SimpleFSDirectory( new File( indexPath ) );
        final IndexWriter indexWriter = new IndexWriter( directory, iwc );

        final AtomicInteger counter = new AtomicInteger();
        br = new BufferedReader( new InputStreamReader( new FileInputStream(csvPath), "UTF-8" ) );
        final LineIterator iter = new LineIterator( br );

        final DateFormat df = new SimpleDateFormat( "EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH );
        
        final ArrayList<String> fails = new ArrayList<String>();

        final ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool( 18, new DaemonThreadFactory() );
        Parallel.forEachPartitioned( new FixedSizeChunkPartitioner<String>( IterableIterator.in( iter ), 1000 ), new Operation<Iterator<String>>() {

            @Override
            public void perform( Iterator<String> it ) {
                int bsize = 0;
                while( it.hasNext() ) {
                    final String line = it.next();
                    final String[] parts = line.split( CSV_REGEX );

                    // 0     1       2   3       4           5    6     7      8     9        10        11     12      13        14        15   16   17
                    // farm, server, id, secret, origsecret, url, path, title, desc, license, uploaded, taken, userid, username, accuracy, lat, lon, tags

                    try {
                        final long flickrId = Long.parseLong( parts[2] );
                        final String userId = parts[12];

                        if( excludeIds.contains( flickrId ) || excludeUsers.contains( userId ) )
                            continue;

                        final String url = parts[5];
                        final String tags = StringUtils.strip( parts[17], "\"[]" ).replaceAll( ", ", " " ).replaceAll("\"", "");
                        final float lat = Float.parseFloat( parts[15] );
                        final float lon = Float.parseFloat( parts[16] );
//                        Date taken = df.parse( parts[11] );
//                        Date uploaded = df.parse( parts[10] );

//                        final Document doc = makeDocument( flickrId, userId, url, tags, taken.getTime(), uploaded.getTime(), lat, lon, ctx, strategy );
                        final Document doc = makeDocument( flickrId, userId, url, tags, lat, lon, ctx, strategy );
                        indexWriter.addDocument( doc );
                    } catch( Throwable t ) {
                        fails.add( parts[2] );
                    }

                    bsize++;
                }
                synchronized( BigLuceneIndexBuilder.class ) {
                    System.out.println( counter.addAndGet( bsize ) );
                }
            }
        }, pool );
        indexWriter.commit();
        indexWriter.close();
        
        System.out.println( "---FAILS---" );
        for( String fail : fails )
            System.out.println( fail );
        System.out.println( "Failed on " + fails.size() + " lines." );
        System.out.println( "Done!" );
    }

    @SuppressWarnings( "deprecation" )
//    private static Document makeDocument( long flickrId, String userId, String url, String tags, long taken, long uploaded, float lat, float lon, SpatialContext ctx, SpatialStrategy strategy ) {
    private static Document makeDocument( long flickrId, String userId, String url, String tags, float lat, float lon, SpatialContext ctx, SpatialStrategy strategy ) {
        final Document doc = new Document();
        
        doc.add( new LongField( FIELD_ID, flickrId, Store.YES ) );
        doc.add( new StringField( FIELD_USER, userId, Store.YES ) );
        doc.add( new StringField( FIELD_URL, url, Store.YES ) );
        doc.add( new TextField( FIELD_TAGS, tags, Store.YES ) );
//        doc.add( new LongField( FIELD_TAKEN, taken, Field.Store.YES ) );
//        doc.add( new LongField( FIELD_UPLOADED, uploaded, Field.Store.YES ) );

        final Shape point = ctx.makePoint( lon, lat );
        for( final IndexableField f : strategy.createIndexableFields( point ) ) {
            doc.add( f );
        }
        doc.add( new StoredField( strategy.getFieldName(), ctx.toString( point ) ) );

        return doc;
    }
}
