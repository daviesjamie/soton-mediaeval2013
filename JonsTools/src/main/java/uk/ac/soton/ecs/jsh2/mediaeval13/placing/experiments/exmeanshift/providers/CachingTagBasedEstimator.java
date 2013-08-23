package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.exmeanshift.providers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.IndexSearcher;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.CachingMeanShiftTagEngine;

public class CachingTagBasedEstimator extends TagBasedEstimator {
	File cacheLocation;

	public CachingTagBasedEstimator(IndexSearcher searcher, File cacheLocation) {
		super(searcher);
		this.cacheLocation = cacheLocation;
	}

	@Override
	protected List<GeoLocation> search(String query) {
		if (query.length() == 0)
			return new ArrayList<GeoLocation>();

		final String subdir;
		if (query.length() >= 4) {
			subdir = query.substring(0, 2) + File.separator + query.substring(2, 4);
		} else if (query.length() >= 2) {
			subdir = query.substring(0, 2) + File.separator + "__";
		} else {
			subdir = "__" + File.separator + "__";
		}

		final File cacheFile = new File(cacheLocation, this.sampleCount + File.separator + subdir + File.separator
				+ query);

		List<GeoLocation> results = read(cacheFile);
		if (results == null) {
			results = super.search(query);
			write(cacheFile, results);
		}
		return results;
	}

	private void write(File cacheFile, List<GeoLocation> results) {
		synchronized (CachingMeanShiftTagEngine.class) {
			if (cacheFile.exists())
				return;

			cacheFile.getParentFile().mkdirs();

			DataOutputStream dos = null;
			try {
				dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)));
				dos.writeInt(results.size());

				for (final GeoLocation r : results) {
					dos.writeDouble(r.latitude);
					dos.writeDouble(r.longitude);
				}
			} catch (final IOException e) {
				e.printStackTrace();
				cacheFile.delete();
			} finally {
				if (dos != null) {
					try {
						dos.close();
					} catch (final IOException e) {
						e.printStackTrace();
						cacheFile.delete();
					}
				}
			}
		}
	}

	private List<GeoLocation> read(File cacheFile) {
		synchronized (CachingMeanShiftTagEngine.class) {
			if (!cacheFile.exists())
				return null;

			final List<GeoLocation> result = new ArrayList<GeoLocation>();

			DataInputStream dis = null;
			try {
				dis = new DataInputStream(new BufferedInputStream(new FileInputStream(cacheFile)));
				final int count = dis.readInt();

				for (int i = 0; i < count; i++) {
					result.add(new GeoLocation(dis.readDouble(), dis.readDouble()));
				}
			} catch (final IOException e) {
				return null;
			} finally {
				try {
					if (dis != null)
						dis.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}

			return result;
		}
	}

	@Override
	public String toString() {
		return "CachingTagBasedEstimator";
	}
}
