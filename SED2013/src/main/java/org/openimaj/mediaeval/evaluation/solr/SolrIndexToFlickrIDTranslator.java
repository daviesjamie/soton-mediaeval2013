package org.openimaj.mediaeval.evaluation.solr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.util.pair.Pair;

public class SolrIndexToFlickrIDTranslator implements Iterable<String>{
	@Option(
		name = "--input",
		aliases = "-i",
		required = true,
		usage = "The input file containing index to clusters, one per line"
	)
	private String input;
	
	@Option(
		name = "--output",
		aliases = "-o",
		required = true,
		usage = "Where to output the corrected file"
	)
	private String ouput;
	
	@Option(
		name = "--lucene-index",
		aliases = "-li",
		required = true,
		usage = "The lucene index used to translate from index to flickr id"
	)
	private String luceneIndex;

	private String[] args;

	private PrintWriter outWriter;

	private BufferedReader inReader;

	private Map<Integer, IndexedPhoto> idmap;
	
	/**
	 * @param args
	 */
	public SolrIndexToFlickrIDTranslator(String[] args) {
		this.args = args;
		prepare();
	}

	private void prepare() {
		final CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
			setup();
		} catch (final Exception e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: java ... [options...] ");
			parser.printUsage(System.err);
			System.exit(1);
		}
	}
	
	private void setup() throws IOException {
		File outf = new File(this.ouput);
		if(!outf.getParentFile().isDirectory() && !outf.getParentFile().mkdirs()) throw new FileNotFoundException("Cannot create parent directory of: " + outf);
		this.outWriter = new PrintWriter(outf);
		this.idmap = SED2013IndexUtils.indexedPhoto(luceneIndex, 0, SED2013IndexUtils.countIndexedItems(luceneIndex));
	}
	
	public void close(){
		this.outWriter.close();
		if(inReader!=null)
			try {
				this.inReader.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		
	}
	
	@Override
	public Iterator<String> iterator() {
		try {
			inReader = new BufferedReader(new InputStreamReader(new FileInputStream(this.input)));
			return new Iterator<String>() {
				String current = inReader.readLine();
				@Override
				public boolean hasNext() {
					return this.current!=null;
				}

				@Override
				public String next() {
					String toRet = this.current;
					try {
						this.current = inReader.readLine();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					return toRet;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Pair<String> lineToFlickrCluster(String line) {
		String[] indexcluster = line.split(" ");
		int index = Integer.parseInt(indexcluster[0]);
		String cluster = indexcluster[1];
		String flickrId = idmap.get(index).second.getId();
		Pair<String> flickerCluster = new Pair<String>(flickrId, cluster);
		return flickerCluster;
	}
	
	public static void main(String[] args) {
		SolrIndexToFlickrIDTranslator opts = new SolrIndexToFlickrIDTranslator(args);
		
		Iterator<String> it = opts.iterator();
		String line = it.next();
		Pair<String> flickerCluster = opts.lineToFlickrCluster(line);
		opts.outWriter.printf("%s %s\n",flickerCluster.firstObject(), flickerCluster.secondObject());
		while (it.hasNext()) {
			line = it.next();
			flickerCluster = opts.lineToFlickrCluster(line);
			opts.outWriter.printf("%s %s\n",flickerCluster.firstObject(), flickerCluster.secondObject());
		}
		opts.close();
	}

	

	
}
