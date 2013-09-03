package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.openimaj.image.FImage;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.util.function.Predicate;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;

public class Blur implements Predicate<ResultItem> {
	private double thresh = 0.85;
	private boolean force = false;

	public Blur() {

	}

	public Blur(double thresh) {
		this.thresh = thresh;
	}

	@Override
	public boolean test(ResultItem object) {
		final File f = new File(object.container.base, "blur" + File.separator
				+ object.container.monument
				+ File.separator + object.id + ".txt");

		double blur;
		if (f.exists() && !force) {
			blur = read(f);
		} else {
			blur = computeBlur(object);

			write(blur, f);
		}

		if (blur < thresh) {
			return false;
		}

		return true;
	}

	private void write(double blur, File f) {
		try {
			f.getParentFile().mkdirs();
			FileUtils.write(f, blur + "");
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private double read(File f) {
		try {
			final String str = FileUtils.readFileToString(f);

			return Double.parseDouble(str);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private double computeBlur(ResultItem object) {
		final FImage img = object.getFImage();

		img.processInplace(new CannyEdgeDetector());

		int count = 0;
		for (int y = 0; y < img.height; y++) {
			for (int x = 0; x < img.width; x++) {
				if (img.pixels[y][x] == 0)
					count++;
			}
		}

		final double p = count / ((double) (img.width * img.height));
		return p;
	}
}
