package org.openimag.mediaeval.placement;

import java.net.UnknownHostException;
import java.util.ArrayList;

import org.openimaj.image.colour.RGBColour;
import org.openimaj.vis.general.DotPlotVisualisation;
import org.openimaj.vis.general.DotPlotVisualisation.ColouredDot;
import org.openimaj.vis.world.WorldMap;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

/**
 * Utilities to plot the position of a group of photos on to
 * a world map using data extracted from a MongoDB instance.
 * 
 * @author Jamie Davies (jagd1g11@ecs.soton.ac.uk)
 * @created 15 Jul 2013
 */
public class MongoPhotoPlotter {

    private final WorldMap<ColouredDot> wp;

    public MongoPhotoPlotter( int width, int height ) {
        this( width, height, false );
    }

    public MongoPhotoPlotter( int width, int height, boolean colouredMap ) {
        wp = new WorldMap<ColouredDot>( width, height, new DotPlotVisualisation() );
        wp.getAxesRenderer().setDrawXAxis( false );
        wp.getAxesRenderer().setDrawYAxis( false );

        if( !colouredMap ) {
            wp.setDefaultCountryLandColour( RGBColour.WHITE );
            wp.setSeaColour( RGBColour.WHITE );
        }
    }

    public void plot( String countryName ) throws UnknownHostException {
        BasicDBObject query = new BasicDBObject( "name", countryName );
        plot( query );
    }

    public void plot( BasicDBObject countryQuery ) throws UnknownHostException {
        MongoClient mongoClient = new MongoClient( "localhost" );
        DB db = mongoClient.getDB( "placement" );

        DBObject country = db.getCollection( "countries" ).findOne( countryQuery );

        countryQuery = new BasicDBObject( "location", new BasicDBObject( "$geoWithin", new BasicDBObject( "$geometry", country.get( "geometry" ) ) ) );
        DBCursor cursor = db.getCollection( "locs" ).find( countryQuery, new BasicDBObject( "location.coordinates", 1 ) );

        plot( cursor );
    }

    public void plot( DBCursor cursor ) {
        int count = 0;

        while( cursor.hasNext() ) {
            ArrayList<Double> coords = (ArrayList<Double>) ((DBObject) (cursor.next().get( "location" ))).get( "coordinates" );
            wp.addPoint( coords.get( 0 ), coords.get( 1 ), new ColouredDot( 0.1d, RGBColour.RED ) );
            count++;
        }

        System.out.println( count + " photos" );

        wp.showWindow( "Photo Distribution" );
    }

    public static void main( String[] args ) throws UnknownHostException {
        MongoPhotoPlotter mp = new MongoPhotoPlotter( 1920, 1200 );
        mp.plot( "New Zealand" );
    }
}
