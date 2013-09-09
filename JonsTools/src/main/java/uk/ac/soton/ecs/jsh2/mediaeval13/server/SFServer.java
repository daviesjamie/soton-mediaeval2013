package uk.ac.soton.ecs.jsh2.mediaeval13.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.openimaj.util.pair.IntLongPair;
import org.openimaj.util.pair.ObjectLongPair;
import org.restlet.Component;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class SFServer extends ServerResource {
	public static void main(String[] args) throws Exception {
		final Component component = new Component();
		// component.getServers().add(Protocol.HTTP, 8182);
		// component.getDefaultHost().attach("/", SFServer.class);
		// component.start();

		loadOffsets("/data/mediaeval/placing/images");
	}

	public static Map<String, Map<String, ObjectLongPair<String>>> offsets = new HashMap<String, Map<String, ObjectLongPair<String>>>();

	public byte[] loadRecord() throws IOException {
		final String remaining = getReference().getRemainingPart();
		final String path = remaining.substring(0, remaining.indexOf(".seq"));
		final String record = remaining.substring(remaining.indexOf(".seq") + 4);

		final ObjectLongPair<String> offset = getOffset(path, record);

		return null;
	}

	private ObjectLongPair<String> getOffset(String path, String record) throws IOException {
		if (!offsets.containsKey(path))
			loadOffsets(path);

		if (!offsets.containsKey(path))
			return null;

		return offsets.get(path).get(record);
	}

	private static synchronized void loadOffsets(String path) throws IOException {
		final Map<String, IntLongPair> offsets = new HashMap<String, IntLongPair>();

		final Configuration conf = new Configuration();
		final Path p = new Path("hdfs://seurat" + path + "-offsets.seq/part-r-00000");
		final FileSystem fs = p.getFileSystem(conf);
		final BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(p)));

		final Pattern pattern = Pattern.compile("(.+).\"location\": \"(.+)\",\"offset\": \"([0-9]+)\"");
		int c = 0;
		String line;
		while ((line = br.readLine()) != null) {
			final Matcher m = pattern.matcher(line);
			m.find();
			final String key = m.group(1).trim();
			final String location = m.group(2);
			final long offset = Long.parseLong(m.group(3));

			final int part = Integer.parseInt(location.substring(location.lastIndexOf("-") + 1));

			offsets.put(key, new IntLongPair(part, offset));

			if (c % 10000 == 0)
				System.out.print(".");
			++c;
		}
		br.close();
	}

	@Override
	@Get
	public String toString() {
		final String remaining = getReference().getRemainingPart();
		final String path = remaining.substring(0, remaining.indexOf(".seq"));
		final String record = remaining.substring(remaining.indexOf(".seq"));

		// Print the requested URI path
		return "Resource URI  : " + getReference() + '\n' + "Root URI      : "
				+ getRootRef() + '\n' + "Routed part   : "
				+ getReference().getBaseRef() + '\n' + "Remaining part: "
				+ getReference().getRemainingPart() + "\n" + path + "\n" + record;
	}
}
