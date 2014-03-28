package uk.ac.soton.ecs.jsh2.mediaeval13.placing.playground;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.geotools.referencing.operation.projection.HotineObliqueMercator;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.processing.transform.ProjectionProcessor;
import org.openimaj.image.renderer.MBFImageRenderer;
import org.openimaj.image.renderer.RenderHints;
import org.openimaj.math.geometry.line.Line2d;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Polygon;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.math.geometry.shape.Shape;
import org.openimaj.math.geometry.transforms.TransformUtilities;
import org.openimaj.math.util.Interpolation;
import org.openimaj.vis.world.WorldPlace;
import org.openimaj.vis.world.WorldPolygons;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.util.Utils;

public class DrawMap {

	private static final File BASE = new File("/Volumes/SSD/mediaeval13/placing/");
	private static final File DEFAULT_LAT_LNG_FILE = new File(BASE, "training_latlng");

	private static final int GRID_WIDTH = 5;
	private static final int BORDER_WIDTH = 4;
	private static final Float[] BG_COLOUR = new Float[] { 30 / 255f, 32 / 255f, 78 / 255f };
	private static final Float[] COUNTRY_BORDER_COLOUR = new Float[] { 90 / 255f, 90 / 255f, 140 / 255f };
	private static final Float[] GRID_COLOUR = new Float[] { 70 / 255f, 70 / 255f, 78 / 255f };

	public static void main(String[] args) throws FileNotFoundException, IOException, FactoryException,
			MismatchedDimensionException, TransformException
	{
		final List<GeoLocation> lls = Utils.readLatLng(DEFAULT_LAT_LNG_FILE, new TLongArrayList());

		final MathTransformFactory fact = new DefaultMathTransformFactory();
		final ParameterValueGroup params = fact.getDefaultParameters("Hotine Oblique Mercator");
		params.parameter("semi_major").setValue(6377397.155);
		params.parameter("semi_minor").setValue(6356078.963);
		params.parameter("longitude_of_center").setValue(7.439583333333333);
		params.parameter("latitude_of_center").setValue(46.952405555555565);
		params.parameter("azimuth").setValue(90.0);
		params.parameter("scale_factor").setValue(1);
		params.parameter("false_easting").setValue(600000.0);
		params.parameter("false_northing").setValue(200000.0);
		params.parameter("rectified_grid_angle").setValue(0.0); // 45

		final HotineObliqueMercator transform = (HotineObliqueMercator) fact.createParameterizedTransform(params);

		double minx = 0;
		double maxx = 0;
		double miny = 0;
		double maxy = 0;
		for (int y = -90; y <= 90; y += 10) {
			for (int x = -180; x <= 180; x += 10) {
				final DirectPosition2D dp = new DirectPosition2D(x, y);
				final double[] pt = transform.transform(dp, (DirectPosition) null).getCoordinate();
				if (pt[0] > maxx)
					maxx = pt[0];
				if (pt[0] < minx)
					minx = pt[0];
				if (pt[1] > maxy)
					maxy = pt[1];
				if (pt[1] < miny)
					miny = pt[1];
			}
		}

		final MBFImage img = new MBFImage(10000, (int) (10000 * ((maxy - miny) / (maxx - minx))), 3);
		img.fill(BG_COLOUR);
		final MBFImageRenderer r = img.createRenderer(RenderHints.ANTI_ALIASED);

		final WorldPolygons wp = new WorldPolygons();
		for (final WorldPlace place : wp.getShapes()) {
			for (final Shape s : place.getShapes()) {
				final Polygon p = transform(s.asPolygon(), transform, minx, maxx, miny, maxy, img.getWidth(),
						img.getHeight()).asPolygon();

				if (p.nVertices() < 2)
					return;

				Point2d p1, p2;
				for (int i = 0; i < p.nVertices() - 1; i++) {
					p1 = p.getVertices().get(i);
					p2 = p.getVertices().get(i + 1);

					if (Line2d.distance(p1, p2) < img.getHeight() / 2)
						r.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY(), BORDER_WIDTH, COUNTRY_BORDER_COLOUR);
				}

				p1 = p.getVertices().get(p.nVertices() - 1);
				p2 = p.getVertices().get(0);
				if (Line2d.distance(p1, p2) < img.getHeight() / 2)
					r.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY(), BORDER_WIDTH, COUNTRY_BORDER_COLOUR);
			}
		}

		final FImage heatmap = new FImage(img.getWidth(), img.getHeight());
		for (final GeoLocation ll : lls) {
			final Point2dImpl pt = new Point2dImpl((float) ll.longitude, (float) ll.latitude);
			final Point2dImpl tpt = transform(transform, minx, maxx, miny, maxy, heatmap.width, heatmap.height, pt);

			final int tx = (int) tpt.x;
			final int ty = (int) tpt.y;

			if (tx >= 0 && tx < heatmap.width && ty >= 0 && ty < heatmap.height)
				heatmap.pixels[ty][tx]++;
		}

		for (int y = 0; y < heatmap.height; y++) {
			for (int x = 0; x < heatmap.width; x++) {
				heatmap.pixels[y][x] = heatmap.pixels[y][x] == 0 ? 0 : (float) Math.log(heatmap.pixels[y][x]);
			}
		}

		// heatmap.processInplace(new FGaussianConvolve(10.0f));
		heatmap.normalise();

		for (int y = 0; y < heatmap.height; y++) {
			for (int x = 0; x < heatmap.width; x++) {
				float val = heatmap.pixels[y][x];
				if (val > 0) {
					val = 0.5f + 0.5f * val;
					final float red = Interpolation.lerp(val, 0, BG_COLOUR[0], 1, 1);
					final float green = Interpolation.lerp(val, 0, BG_COLOUR[1], 1, 1);
					final float blue = Interpolation.lerp(val, 0, BG_COLOUR[2], 1, 1);

					for (int yy = -1; yy <= 1; yy++)
						for (int xx = -1; xx <= 1; xx++)
							img.setPixel(x + xx, y + yy, new Float[] { red, green, blue });
				}
			}
		}

		for (int y = -90; y <= 90; y += 15) {
			Point2dImpl last = transform(transform, minx, maxx, miny, maxy, img.getWidth(), img.getHeight(),
					new Point2dImpl(-180, y));

			for (float x = -180; x <= 180; x += 0.1) {
				final Point2dImpl pt = transform(transform, minx, maxx, miny, maxy, img.getWidth(), img.getHeight(),
						new Point2dImpl(x, y));
				if (Line2d.distance(last, pt) < img.getHeight() / 2)
					r.drawLine(last, pt, GRID_WIDTH, GRID_COLOUR);
				last = pt;
			}
		}

		for (int x = -180; x <= 180; x += 15) {
			Point2dImpl last = transform(transform, minx, maxx, miny, maxy, img.getWidth(), img.getHeight(),
					new Point2dImpl(x, -90));

			for (float y = -90; y <= 90; y += 0.1f) {
				final Point2dImpl pt = transform(transform, minx, maxx, miny, maxy, img.getWidth(), img.getHeight(),
						new Point2dImpl(x, y));
				if (Line2d.distance(last, pt) < img.getHeight() / 2)
					r.drawLine(last, pt, GRID_WIDTH, GRID_COLOUR);
				last = pt;
			}
		}

		MBFImage bigimg = new MBFImage(img.getWidth(), (int) (img.getHeight() * 1.5), 3);
		bigimg.drawImage(img, 0, -img.getHeight() / 2);
		bigimg.drawImage(img, 0, img.getHeight() / 2);

		bigimg = ProjectionProcessor.project(bigimg, TransformUtilities.rotationMatrix(Math.PI / 4), BG_COLOUR);

		bigimg = bigimg.extractROI(new Rectangle(
				(int) (bigimg.getWidth() / 3.15),
				(int) (bigimg.getHeight() / 3.5),
				(int) (0.65f * img.getWidth()),
				(int) (0.65f * img.getWidth() * Math.sqrt(2))));

		ImageUtilities.write(bigimg, new File("/Users/jsh2/map.png"));
	}

	private static Shape transform(Polygon asPolygon, HotineObliqueMercator transform, double minx, double maxx,
			double miny, double maxy, int width, int height) throws TransformException
	{
		final Polygon p = new Polygon();

		for (final Point2d pt : asPolygon.points) {
			final Point2dImpl pto = transform(transform, minx, maxx, miny, maxy, width, height, pt);

			p.points.add(pto);
		}

		return p;
	}

	private static Point2dImpl transform(HotineObliqueMercator transform, double minx, double maxx, double miny,
			double maxy, int width, int height, final Point2d pt) throws TransformException
	{
		final float x = pt.getX();
		final float y = pt.getY();
		final double[] c = transform.transform(new DirectPosition2D(x, y), (DirectPosition) null)
				.getCoordinate();

		final float px = (float) (width * (c[0] - minx) / (maxx - minx));
		final float py = (float) (height - height * (c[1] - miny) / (maxy - miny));
		final Point2dImpl pto = new Point2dImpl(px, py);
		return pto;
	}
}
