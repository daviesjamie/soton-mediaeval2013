package org.openimag.mediaeval.placement;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.openimaj.image.colour.RGBColour;
import org.openimaj.vis.general.DotPlotVisualisation;
import org.openimaj.vis.general.DotPlotVisualisation.ColouredDot;
import org.openimaj.vis.world.WorldMap;


/**
 * Plots each photo's location (in a subset of the data) on a world map.
 * 
 * @author Jamie Davies (jagd1g11@ecs.soton.ac.uk)
 * @created 09 Jul 2013
 */
public class PhotoDistribution {

    public static void main( String[] args ) throws IOException, SQLException {
        final WorldMap<ColouredDot> wp = new WorldMap<ColouredDot>(1920, 1200, new DotPlotVisualisation() );
        wp.getAxesRenderer().setDrawXAxis( false );
        wp.getAxesRenderer().setDrawYAxis( false );
        wp.setDefaultCountryLandColour( RGBColour.WHITE );
        wp.setSeaColour( RGBColour.WHITE );
        
        Connection c = DriverManager.getConnection( "jdbc:mysql://localhost/Placement?user=root" );
        Statement s = c.createStatement();
        
        StringBuilder sb = new StringBuilder();
        
        //sb.append( "SELECT latitude, longitude FROM image_locations " );
        //sb.append( "WHERE ST_Contains(" );
        //sb.append( "(SELECT polygon FROM country_polygons WHERE countryName='Vietnam'), coords" );
        //sb.append( ") = 1;" );
        
        sb.append( "SELECT latitude, longitude FROM vietnamese_images;" );
        
        ResultSet resultSet = s.executeQuery( sb.toString() );
        int resultCount = 0;
        
        if( resultSet != null ) {
            while( resultSet.next() ) {
                wp.addPoint( Float.parseFloat( resultSet.getString( 2 ) ), Float.parseFloat( resultSet.getString( 1 ) ), new ColouredDot( 0.1d, RGBColour.RED ) );
                resultCount++;
            }
        }
        
        c.close();
        
        System.out.println( resultCount + " photos found." );
        
        wp.showWindow("Photo Distribution");
    }
}
