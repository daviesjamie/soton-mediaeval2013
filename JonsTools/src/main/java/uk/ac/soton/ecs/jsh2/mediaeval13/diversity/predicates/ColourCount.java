package uk.ac.soton.ecs.jsh2.mediaeval13.diversity.predicates;

import java.io.File;
import java.io.IOException;

import org.openimaj.feature.DoubleFV;
import org.openimaj.image.MBFImage;
import org.openimaj.image.pixel.statistics.HistogramModel;
import org.openimaj.io.IOUtils;
import org.openimaj.util.array.ArrayUtils;
import org.openimaj.util.function.Predicate;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.ResultItem;

public class ColourCount implements Predicate<ResultItem> {
	private double thresh = 0.4;
	private boolean force = false;

	public ColourCount() {

	}

	public ColourCount(double thresh) {
		this.thresh = thresh;
	}

	@Override
	public boolean test(ResultItem object) {
		final File f = new File(object.container.base, "colhist6x6x6" + File.separator
				+ object.container.monument
				+ File.separator + object.id + ".txt");

		DoubleFV hist;
		if (f.exists() && !force) {
			hist = read(f);
		} else {
			hist = computeHist(object);

			write(hist, f);
		}

		if (ArrayUtils.maxValue(hist.values) > thresh) {
			return false;
		}

		return true;
	}

	private void write(DoubleFV hist, File f) {
		try {
			f.getParentFile().mkdirs();
			IOUtils.writeASCII(f, hist);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private DoubleFV read(File f) {
		try {
			return IOUtils.read(f, DoubleFV.class);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private DoubleFV computeHist(ResultItem object) {
		final MBFImage img = object.getMBFImage();

		final HistogramModel hm = new HistogramModel(6, 6, 6);
		hm.estimateModel(img);

		return hm.histogram.normaliseFV();
	}
}
