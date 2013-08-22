package org.openimaj.mediaeval.placement.vis;

import org.openimaj.image.MBFImage;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.mediaeval.placement.vis.PointPlotVisualisation.ColouredPoint;
import org.openimaj.vis.general.AxesRenderer2D;
import org.openimaj.vis.general.ItemPlotter;
import org.openimaj.vis.general.XYPlotVisualisation;

/**
 *  Plots single points at set coordinates.
 *
 *  @author Jamie Davies (jagd1g11@ecs.soton.ac.uk)
 */
public class PointPlotVisualisation extends XYPlotVisualisation<ColouredPoint> implements ItemPlotter<ColouredPoint, Float[], MBFImage> {
    
    private static final long serialVersionUID = 1L;
    
    private static final Float[] DEFAULTPOINTCOLOUR = { 1f, 0f, 0f, 1f };

    /**
     *  A single point with a specific colour.
     *
     *  @author Jamie Davies (jagd1g11@ecs.soton.ac.uk)
     */
    public static class ColouredPoint {
        public Float[] colour;
        
        public ColouredPoint( Float[] colour ) {
            this.colour = colour;
        }
    }
    
    /**
     *  Default constructor
     */
    public PointPlotVisualisation()
    {
        super( null );
        this.setItemPlotter( this );
    }
    
    /**
     *  Constructor that takes the width and height of the visualisation
     *
     *  @param width The width of the visualisation in pixels
     *  @param height The height of the visualisation in pixels
     */
    public PointPlotVisualisation( int width, int height ) {
        super( width, height, null );
        this.setItemPlotter( this );
    }
    
    /**
     *  Adds a point at the given location with the default colour.
     *  @param x The x location
     *  @param y The y location
     */
    public void addPoint( final double x, final double y )
    {
        super.addPoint( x, y, new ColouredPoint( DEFAULTPOINTCOLOUR ) );
    }
    
    /**
     *  Adds a point at the given location with the given colour.
     *  @param x The x location
     *  @param y The y location
     *  @param colour The point colour 
     */
    public void addPoint( final double x, final double y, final Float[] colour )
    {
        super.addPoint( x, y, new ColouredPoint( colour ) );
    }

    /**
     *  {@inheritDoc}
     *  @see org.openimaj.vis.general.ItemPlotter#renderRestarting()
     */
    @Override
    public void renderRestarting() {
    }

    /**
     *  {@inheritDoc}
     *  @see org.openimaj.vis.general.ItemPlotter#plotObject(org.openimaj.image.Image, org.openimaj.vis.general.XYPlotVisualisation.LocatedObject, org.openimaj.vis.general.AxesRenderer2D)
     */
    @Override
    public void plotObject( MBFImage visImage, org.openimaj.vis.general.XYPlotVisualisation.LocatedObject<ColouredPoint> object, AxesRenderer2D<Float[], MBFImage> renderer ) {
        Point2d pos = renderer.calculatePosition( object.x, object.y );
        visImage.setPixel( (int)pos.getX(), (int)pos.getY(), object.object.colour );
    }
}
