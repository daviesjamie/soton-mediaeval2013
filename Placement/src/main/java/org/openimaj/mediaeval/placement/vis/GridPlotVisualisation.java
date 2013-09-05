package org.openimaj.mediaeval.placement.vis;

import org.openimaj.image.MBFImage;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.shape.Polygon;
import org.openimaj.mediaeval.placement.vis.GridPlotVisualisation.ColouredSquare;
import org.openimaj.vis.general.AxesRenderer2D;
import org.openimaj.vis.general.ItemPlotter;
import org.openimaj.vis.general.XYPlotVisualisation;

/**
 * Plots squares/cells of a grid.
 * 
 * @author Jamie Davies (jagd1g11@ecs.soton.ac.uk)
 */
public class GridPlotVisualisation extends XYPlotVisualisation<ColouredSquare> implements
		ItemPlotter<ColouredSquare, Float[], MBFImage> {

	private static final long serialVersionUID = 1L;

	private static final Float[] DEFAULTPOINTCOLOUR = { 1f, 0f, 0f, 1f };

	private final double gridSpacing;

	/**
	 * A grid cell with a specific colour.
	 * 
	 * @author Jamie Davies (jagd1g11@ecs.soton.ac.uk)
	 */
	public static class ColouredSquare {
		public Float[] colour;

		public ColouredSquare(Float[] colour) {
			this.colour = colour;
		}
	}

	/**
	 * Basic constructor
	 */
	public GridPlotVisualisation(double gridSpacing)
	{
		super(null);
		this.setItemPlotter(this);
		this.gridSpacing = gridSpacing;
	}

	/**
	 * Constructor that takes the width and height of the visualisation
	 * 
	 * @param width
	 *            The width of the visualisation in pixels
	 * @param height
	 *            The height of the visualisation in pixels
	 */
	public GridPlotVisualisation(int width, int height, double gridSpacing) {
		super(width, height, null);
		this.setItemPlotter(this);
		this.gridSpacing = gridSpacing;
	}

	/**
	 * Adds a point at the given location with the default colour.
	 * 
	 * @param x
	 *            The x location
	 * @param y
	 *            The y location
	 */
	public void addPoint(final double x, final double y)
	{
		super.addPoint(x, y, new ColouredSquare(DEFAULTPOINTCOLOUR));
	}

	/**
	 * Adds a point at the given location with the given colour.
	 * 
	 * @param x
	 *            The x location
	 * @param y
	 *            The y location
	 * @param colour
	 *            The point colour
	 */
	public void addPoint(final double x, final double y, final Float[] colour)
	{
		super.addPoint(x, y, new ColouredSquare(colour));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.openimaj.vis.general.ItemPlotter#renderRestarting()
	 */
	@Override
	public void renderRestarting() {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.openimaj.vis.general.ItemPlotter#plotObject(org.openimaj.image.Image,
	 *      org.openimaj.vis.general.XYPlotVisualisation.LocatedObject,
	 *      org.openimaj.vis.general.AxesRenderer2D)
	 */
	@Override
	public void plotObject(MBFImage visImage,
			org.openimaj.vis.general.XYPlotVisualisation.LocatedObject<ColouredSquare> object,
			AxesRenderer2D<Float[], MBFImage> renderer) {
		Point2d topLeft = renderer.calculatePosition(object.x, object.y);
		Point2d topRight = renderer.calculatePosition(object.x + gridSpacing, object.y);
		Point2d bottomLeft = renderer.calculatePosition(object.x, object.y + gridSpacing);
		Point2d bottomRight = renderer.calculatePosition(object.x + gridSpacing, object.y + gridSpacing);

		visImage.drawPolygonFilled(new Polygon(topLeft, topRight, bottomRight, bottomLeft), object.object.colour);
	}
}
