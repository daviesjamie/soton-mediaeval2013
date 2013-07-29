package org.openimaj.mediaeval.placement.utils;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.openimaj.text.nlp.language.LanguageDetector;
import org.openimaj.text.nlp.language.LanguageDetector.WeightedLocale;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

/**
 * Imports photo data for the Placement task into Solr from a MongoDB database.
 * 
 * @author Jamie Davies (jagd1g11@ecs.soton.ac.uk)
 */
public class MongoToSolrImporter {

    /** Use log4j logging facilities */
    private static Logger log = Logger.getLogger( MongoToSolrImporter.class );

    /** The number of documents to add before committing to the Solr server */
    private static final int BATCH_SIZE = 20000;

    /** The Solr server to push the data to */
    private HttpSolrServer solrServer;

    /** The MongoDB database to pull the data from */
    private DB mongo;

    /**
     * Basic constructor. Instantiates the Solr server and MongoDB database.
     * 
     * @param mongoHost
     *            The name of the host the MongoDB server is running on.
     * @param mongoDB
     *            The name of the MongoDB database to use.
     * @param solrURL
     *            The URL of the Solr collection.
     * @throws Exception
     *             If there is an error connecting to MongoDB or Solr.
     */
    public MongoToSolrImporter( String mongoHost, String mongoDB, String solrURL ) throws Exception {
        MongoClient mongoClient = new MongoClient( mongoHost );
        mongo = mongoClient.getDB( mongoDB );

        solrServer = new HttpSolrServer( solrURL );
    }

    /**
     * Force a commit against the Solr server.
     */
    private void commit() {
        try {
            solrServer.commit();
        } catch( Exception ex ) {
            log.error( "Failed to commit: ", ex );
        }
    }

    /**
     * Force an optimize call against the Solr server.
     */
    private void optimize() {
        try {
            solrServer.optimize();
        } catch( Exception ex ) {
            log.error( "Failed to optimize: ", ex );
        }
    }

    /**
     * The main transfer loop for the program.<br><br>
     * 
     * Loops through all images in the MongoDB 'images' collection, queries the
     * 'locs' collection to get the location data for each image, constructs a
     * {@link SolrInputDocument} containing data for the image and then adds
     * this to the Solr server. A commit is forced on the Solr server when <i>
     * {@link BATCH_SIZE}</i> images have been added.
     * 
     * @throws SolrServerException
     *             If there is an error communicating with the Solr server.
     * @throws IOException
     *             If there is an error adding a document to the Solr server.
     */
    private void transfer() throws SolrServerException, IOException {
        DBCursor images = mongo.getCollection( "images" ).find();

        int current = 0;
        
        LanguageDetector ld = new LanguageDetector();

        while( images.hasNext() ) {
            DBObject image = images.next();
            DBObject loc = mongo.getCollection( "locs" ).findOne( new BasicDBObject( "photoID", image.get( "photoID" ) ) );

            // Only add image if it has lat/long data
            if( loc != null ) {
                SolrInputDocument doc = new SolrInputDocument();
                doc.addField( "photoID", image.get( "photoID" ) );
                doc.addField( "photoTags", image.get( "photoTags" ) );

                ArrayList<Double> coords = MongoUtils.extractLatLong( loc );
                doc.addField( "location", coords.get( 1 ) + "," + coords.get( 0 ) );
                
                // Guestimate the language used in the tags
                WeightedLocale langresult = ld.classify( image.get( "photoTags" ).toString() );
                
                doc.addField( "langID", langresult.language );
                doc.addField( "langConfidence", langresult.confidence );
                
                solrServer.add( doc );
                
                if( current >= BATCH_SIZE ) {
                    log.info( "Committing " + current + " images" );
                    commit();
                    current = 0;
                }
                
                current++;
            }
        }

        log.info( "Committing " + current + " images" );
        commit();
        log.info( "Optimizing..." );
        optimize();
        log.info( "Import complete." );
    }

    public static void main( String[] args ) throws Exception {
        MongoToSolrImporter mtsi = new MongoToSolrImporter( "localhost", "placement", "http://localhost:8983/solr/placementimages" );
        mtsi.transfer();
    }
}
