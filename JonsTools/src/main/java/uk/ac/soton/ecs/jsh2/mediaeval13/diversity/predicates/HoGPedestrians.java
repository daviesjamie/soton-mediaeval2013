package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.util.function.Predicate;
import org.openimaj.util.iterator.TextLineIterable;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;

public class HoGPedestrians implements Predicate<ResultItem> {
	double detectionThresh = 0;
	int numThresh = 0;

	public static List<ObjectDoublePair<Rectangle>> getDetections(ResultItem object) {
		final List<ObjectDoublePair<Rectangle>> rects = new ArrayList<ObjectDoublePair<Rectangle>>();

		final File f = new File(object.container.base, "ocv-hog-person" + File.separator + object.container.monument
				+ File.separator + object.id + ".txt");

		if (f.exists()) {
			for (final String line : new TextLineIterable(f)) {
				final String[] parts = line.split(" ");

				final Rectangle r = new Rectangle(
						Integer.parseInt(parts[0]),
						Integer.parseInt(parts[1]),
						Integer.parseInt(parts[2]),
						Integer.parseInt(parts[3]));

				rects.add(ObjectDoublePair.pair(r, Double.parseDouble(parts[4])));
			}
		}

		return rects;
	}

	@Override
	public boolean test(ResultItem object) {
		int count = 0;

		for (final ObjectDoublePair<Rectangle> r : getDetections(object)) {
			if (r.second > detectionThresh)
				count++;
		}

		return !(count > numThresh);
	}

}
