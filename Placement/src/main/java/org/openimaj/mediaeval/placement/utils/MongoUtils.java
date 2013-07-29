package org.openimaj.mediaeval.placement.utils;

import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;


public class MongoUtils {
    
    /** Use log4j logging facilities */
    private static Logger log = Logger.getLogger( MongoUtils.class );
    
    public static final String MONGO_HOST = "localhost";
    public static final String MONGO_DB = "placement";
    public static final String MONGO_POLYGON_COLLECTION = "countries";
    public static final String MONGO_META_COLLECTION = "images";
    public static final String MONGO_LOCS_COLLECTION = "locs";
    
    public static DB getDB() throws UnknownHostException {
        log.info( "Creating connection to Mongo DB at " + MONGO_HOST + "." + MONGO_DB );
        MongoClient mongoClient = new MongoClient( MONGO_HOST );
        return mongoClient.getDB( MONGO_DB );
    }
    
    public static Object getCountryPolygon( String countryName, DB db ) {
        log.info( "Getting polygon for " + countryName + " from " + MONGO_DB + "." + MONGO_POLYGON_COLLECTION );
        DBObject query = new BasicDBObject( "name", countryName );
        return db.getCollection( MONGO_POLYGON_COLLECTION ).findOne( query ).get( "geometry" );
    }
    
    public static DBObject findOne( DBObject query, DBObject filter, DBCollection collection ) {
        log.info( "Executing findOne() query on " + MONGO_DB + "." + collection.getName() );
        return collection.findOne( query, filter );
    }
    
    public static DBCursor find( DBObject query, DBObject filter, DBCollection collection ) {
        log.info( "Executing find() query on " + MONGO_DB + "." + collection.getName() );
        return collection.find( query, filter );
    }
    
    public static ArrayList<Double> extractLatLong( DBObject object ) {
        return (ArrayList<Double>) ((DBObject) (object.get( "location" ))).get( "coordinates" );
    }
    
}
