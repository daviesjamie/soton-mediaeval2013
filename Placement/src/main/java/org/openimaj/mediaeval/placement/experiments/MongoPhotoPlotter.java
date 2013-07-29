package org.openimaj.mediaeval.placement.experiments;

import java.net.UnknownHostException;

import org.openimaj.mediaeval.placement.utils.MongoUtils;
import org.openimaj.mediaeval.placement.vis.PhotoPlot;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class MongoPhotoPlotter {
    
    private static final String COUNTRY_NAME = "Morocco";
    private static final boolean STREAM = false;
    
    public static void main( String[] args ) {
        try {
            PhotoPlot pp = new PhotoPlot( 1920, 1200 );
            
            DB db = MongoUtils.getDB();
            Object poly = MongoUtils.getCountryPolygon( COUNTRY_NAME, db );
            DBObject query = new BasicDBObject( "location", new BasicDBObject( "$geoWithin", poly ) );
            
            if( STREAM ) {
                DBCursor cursor = MongoUtils.find( query, null, db.getCollection( MongoUtils.MONGO_LOCS_COLLECTION) );
                pp.addPhotos( cursor );
            } else {
                pp.addPhotos( MongoUtils.find( query, null, db.getCollection( MongoUtils.MONGO_LOCS_COLLECTION ) ) );
            }
            
            pp.display();
            
        } catch( UnknownHostException e ) {
            System.err.println( "Error connecting to MongoDB." );
            e.printStackTrace();
        }
    }
}
