package org.openimaj.mediaeval.placement.vis;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.mediaeval.placement.data.Photo;
import org.openimaj.mediaeval.placement.utils.MongoUtils;
import org.openimaj.mediaeval.placement.vis.PointPlotVisualisation.ColouredPoint;
import org.openimaj.util.function.Operation;
import org.openimaj.util.stream.Stream;
import org.openimaj.vis.general.DotPlotVisualisation;
import org.openimaj.vis.general.DotPlotVisualisation.ColouredDot;
import org.openimaj.vis.world.WorldMap;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Visualise photo locations by plotting them on a world map.
 * 
 * @author Jamie Davies (jagd1g11@ecs.soton.ac.uk)
 */
public class PhotoPlot {
    
    private static final Float[] POINT_COLOUR = { 1f, 0f, 0f, 1f };

    /** Use log4j logging facilities */
    private static Logger log = Logger.getLogger( PhotoPlot.class );

    /** The map to draw the photos on */
    private final WorldMap<ColouredPoint> wp;

    /**
     * Initialises a colourless world map with the given dimensions.
     * 
     * @param width
     *            The width of the map (in pixels).
     * @param height
     *            The height of the map (in pixels).
     */
    public PhotoPlot( int width, int height ) {
        this( width, height, false );
    }

    /**
     * Initialises a world map with the given dimensions and optional colour.
     * 
     * @param width
     *            The width of the map (in pixels).
     * @param height
     *            The height of the map (in pixels).
     * @param colour
     *            Whether to render the map in colour.
     */
    public PhotoPlot( int width, int height, boolean colour ) {
        log.info( "Creating World Map" );

        wp = new WorldMap<ColouredPoint>( width, height, new PointPlotVisualisation() );
        wp.getAxesRenderer().setDrawXAxis( false );
        wp.getAxesRenderer().setDrawYAxis( false );

        if( !colour ) {
            wp.setDefaultCountryLandColour( RGBColour.WHITE );
            wp.setSeaColour( RGBColour.WHITE );
        }
    }

    /**
     * Adds a photo location to the map.
     * 
     * @param latitude
     *            The latitude of the photo.
     * @param longitude
     *            The longitude of the photo.
     */
    public void addPhoto( double latitude, double longitude ) {
        wp.addPoint( longitude, latitude, new ColouredPoint( POINT_COLOUR ) );
    }

    /**
     * Adds a photo to the map.
     * 
     * @param photo
     *            The {@link Photo} to add to the map.
     */
    public void addPhoto( Photo photo ) {
        wp.addPoint( photo.getLongitude(), photo.getLatitude(), new ColouredPoint( POINT_COLOUR ) );
    }

    /**
     * Adds all photos in a stream to the map.
     * 
     * @param photos
     *            The stream of {@link Photo}s to plot, such as that from a
     *            {@link MongoPhotoStream}.
     */
    public void addPhotos( Stream<Photo> photos ) {
        log.info( "Adding photos in photo stream" );
        photos.forEach( new Operation<Photo>() {

            @Override
            public void perform( Photo object ) {
                addPhoto( object );
            }
        } );
    }

    /**
     * Adds all photos pointed to by a MongoDB cursor.
     * 
     * @param photoCursor
     *            The cursor returned from a query on a MongoDB collection.
     */
    public void addPhotos( DBCursor photoCursor ) {
        log.info( "Adding photos from query cursor" );
        while( photoCursor.hasNext() ) {
            DBObject p = photoCursor.next();
            ArrayList<Double> coords = MongoUtils.extractLatLong( p );
            addPhoto( coords.get( 1 ), coords.get( 0 ) );
        }
    }

    /**
     * Renders the map and all of the photo locations.
     */
    public void display() {
        log.info( "Rendering map" );
        wp.showWindow( "Photo Plot" );
    }

}
