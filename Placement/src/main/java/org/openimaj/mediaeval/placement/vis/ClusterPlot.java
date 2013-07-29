package org.openimaj.mediaeval.placement.vis;

import org.apache.log4j.Logger;
import org.openimaj.image.colour.ColourMap;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.vis.general.DotPlotVisualisation;
import org.openimaj.vis.general.DotPlotVisualisation.ColouredDot;
import org.openimaj.vis.world.WorldMap;

/**
 * Visualise cluster locations and sizes by drawing them on a world map.
 * 
 * @author Jamie Davies (jagd1g11@ecs.soton.ac.uk)
 */
public class ClusterPlot {

    private static final double POINT_SIZE = 0.5; 
    
    /** Use log4j logging facilities */
    private static Logger log = Logger.getLogger( ClusterPlot.class );

    /** The map to draw the clusters on */
    private final WorldMap<ColouredDot> wp;

    /** A ColourMap to use to assign colours to the clusters */
    private ColourMap colourMap;

    /** Colour map range */
    private double colourMapMin;

    /** Colour map range */
    private double colourMapMax;

    /**
     * Initialises a colourless world map with the given dimensions.
     * 
     * @param width
     *            The width of the map (in pixels).
     * @param height
     *            The height of the map (in pixels).
     */
    public ClusterPlot( int width, int height ) {
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
    public ClusterPlot( int width, int height, boolean colour ) {
        log.info( "Creating World Map" );

        colourMap = ColourMap.Autumn;
        colourMapMin = 1.5;
        colourMapMax = 10;

        wp = new WorldMap<DotPlotVisualisation.ColouredDot>( width, height, new DotPlotVisualisation() );
        wp.getAxesRenderer().setDrawXAxis( false );
        wp.getAxesRenderer().setDrawYAxis( false );

        if( !colour ) {
            wp.setDefaultCountryLandColour( RGBColour.WHITE );
            wp.setSeaColour( RGBColour.WHITE );
        }
    }

    public void addCluster( double latitude, double longitude, double diameter ) {
        Float[] c = this.colourMap.apply( (float)((diameter-this.colourMapMin)/(this.colourMapMax-this.colourMapMin)) );
        Float[] ac = new Float[4];
        ac[ 0 ] = c[ 0 ];
        ac[ 1 ] = c[ 1 ];
        ac[ 2 ] = c[ 2 ];
        ac[ 3 ] = 0.5f;
        
        wp.addPoint( longitude, latitude, new ColouredDot( diameter, ac ) );
    }
    
    public void addPoint( double latitude, double longitude ) {
        Float[] c = RGBColour.GREEN;
        wp.addPoint( longitude, latitude, new ColouredDot( POINT_SIZE, c ) );
    }

    /**
     * Renders the map and all of the photo locations.
     */
    public void display() {
        log.info( "Rendering map" );
        wp.showWindow( "Photo Plot" );
    }

}
