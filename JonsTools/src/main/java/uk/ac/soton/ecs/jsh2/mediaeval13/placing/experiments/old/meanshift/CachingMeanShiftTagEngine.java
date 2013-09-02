package uk.ac.soton.ecs.jsh2.mediaeval13.placing.experiments.old.meanshift;

import gnu.trove.list.array.TLongArrayList;

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

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation.GeoLocation;

public class CachingMeanShiftTagEngine extends MeanShiftTagEngine {
	static File CACHE_DIR = new File("/Volumes/SSD/tags-cache/");

	public CachingMeanShiftTagEngine(File file, TLongArrayList skipIds, int sampleCount, File latlngFile, double bandwidth)
			throws IOException
	{
		super(file, skipIds, sampleCount, latlngFile, bandwidth);
	}

	@Override
	protected List<GeoLocation> search(String query) {
		if (query.length() == 0)
			return new ArrayList<GeoLocation>();

		final String l1 = query.length() >= 2 ? query.substring(0, 2) : "00";
		final String l2 = query.length() >= 4 ? query.substring(2, 4) : "00";
		final File cacheFile = new File(CACHE_DIR, this.sampleCount + File.separator + l1 + File.separator + l2
				+ File.separator + query);

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
}
