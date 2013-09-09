package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineOptionsProvider;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ProxyOptionHandler;
import org.mortbay.io.RuntimeIOException;
import org.openimaj.io.IOUtils;
import org.openimaj.logger.LoggerUtils;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.util.function.Function;
import org.openimaj.util.function.Operation;
import org.openimaj.util.stream.AbstractStream;

import com.Ostermiller.util.CSVParser;

import ch.akuhn.matrix.SparseMatrix;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SED2013ImageSimilarityMatrixTool {
	private static Logger logger = Logger.getLogger(SED2013ImageSimilarityMatrixTool.class);

	@Option(
		name = "--lucene-index",
		aliases = "-li",
		required = true,
		usage = "The solr index from which to extract image row index from flickr id"
	)
	private String solrInput;
	
	@Option(
		name = "--force-http",
		aliases = "-h",
		required = false,
		usage = "The provided solr index is treated as a URL to a solr server"
	)
	private boolean forceHTTP;
	
	@Option(
		name = "--incremental-build",
		aliases = "-inc",
		required = false,
		usage = " if set to a positive number, split the final matrix as a number of matricies in a directory. Each matrix will be of the same dimensions, but will only contain inc completed rows."
	)
	private int incBuild = -1;
	
	@Option(
		name = "--image-graph-file",
		aliases = "-i",
		required = true,
		usage = "A CSV file containing lines like: image1 image2 similarity"
	)
	private String input;
	
	@Option(
		name = "--simmat-output",
		aliases = "-o",
		required = true,
		usage = "The root to save all the similarity matricies"
	)
	private String output;
	static interface NormModeOption{
		public void setup(SED2013ImageSimilarityMatrixTool tool);
		public double normalise(double v);
	}
	enum NormMode implements CmdLineOptionsProvider {
		MAX {
			@Override
			public NormModeOption getOptions() {
				return new NormModeOption() {
					double max = 0;
					@Override
					public void setup(SED2013ImageSimilarityMatrixTool tool) {
						try {
							logger.info("First pass, finding max");
							max = tool.findMaxValue();
						} catch (IOException e) {
							throw new RuntimeIOException(e);
						}
					}
					
					@Override
					public double normalise(double v) {
						return v/max;
					}
					
					public String toString() {
						return String.format("max_%.2f",max);
					};
				};
			}
		},
		
		LOG {
			@Override
			public NormModeOption getOptions() {
				return new NormModeOption() {
					@Option(
						name = "--log-max",
						aliases = "-lm",
						required = false,
						usage = "All values will be = log(value)/log(log-max) clipped at 0 and 1"
					)
					double logmax = 10;
					@Override
					public void setup(SED2013ImageSimilarityMatrixTool tool) {
						
					}
					
					@Override
					public double normalise(double v) {
						return clip(Math.log(v)/Math.log(logmax),0,1);
					}

					private double clip(double d, int min, int max) {
						if(d < min) return min;
						if(d > max) return max;
						return d;
					}
					public String toString() {
						return String.format("logmax_%.2f",logmax);
					};
				};
			}
		},
		BINARY {
			@Override
			public NormModeOption getOptions() {
				return new NormModeOption() {
					@Option(
						name = "--threshold",
						aliases = "-tr",
						required = false,
						usage = "Below this value, 0, above this value 1"
					)
					double thresh = 0;
					@Override
					public void setup(SED2013ImageSimilarityMatrixTool tool) {
						
					}
					
					@Override
					public double normalise(double v) {
						return v < thresh ? 0 : 1;
					}
					
					public String toString() {
						return String.format("binary_%.2f",thresh);
					};
				};
			}
		};

		@Override
		public abstract Object getOptions() ;
		
	}
	
	@Option(
		name = "--norm-mode",
		aliases = "-nm",
		required = true,
		usage = "How to normalise the matrix",
		handler = ProxyOptionHandler.class
	)
	private NormMode nm = NormMode.MAX;
	private NormModeOption nmOp = null;
	
	Pattern imgPattern = Pattern.compile(".+?/(.*)[.]jpg .+?/(.*)[.]jpg");
	private String[] args;

	private int nIndexes;

	private Map<String, IndexedPhoto> indexMap;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws CorruptIndexException 
	 */
	public SED2013ImageSimilarityMatrixTool(String[] args) throws CorruptIndexException, IOException {
		this.args = args;
		this.prepare();
	}

	private void prepare() throws CorruptIndexException, IOException {
		final CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
			logger.debug("Counting index size");
			this.nIndexes = SED2013IndexUtils.countIndexedItems(solrInput);
			logger.debug("Constructing flickrid to index map");
			this.indexMap = SED2013IndexUtils.flickrIDIndexedPhoto(solrInput,0,nIndexes);
			this.nmOp.setup(this);
		} catch (final CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: java ... [options...] ");
			parser.printUsage(System.err);
			System.exit(1);
		}
	}
	private static class ImageGraphEdge{
		public ImageGraphEdge(String img1, String img2, double parseDouble) {
			this.img1 = img1;
			this.img2 = img2;
			this.value = parseDouble;
		}
		String img1;
		String img2;
		double value;
	}
	
	class ImageGraphStream extends AbstractStream<ImageGraphEdge>{
		
		private CSVParser parser;
		private String[] nextLine;

		public ImageGraphStream(File f) throws IOException {
			this(new FileInputStream(f));
		}
		
		public ImageGraphStream(InputStream ios) throws IOException {
			this.parser = new CSVParser(ios, '\t');
			this.nextLine = this.parser.getLine();
		}

		@Override
		public boolean hasNext() {
			return this.nextLine!=null;
		}

		@Override
		public ImageGraphEdge next() {
			String[] toRet = this.nextLine;
			try {
				this.nextLine = this.parser.getLine();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			Matcher matcher = imgPattern.matcher(toRet[0]);
			if(matcher.matches()){				
				String img1 = matcher.group(1);
				String img2 = matcher.group(2);
				return new ImageGraphEdge(img1,img2,Double.parseDouble(toRet[1]));
			}
			throw new RuntimeException("line does not match!");
		}
		
	}
	public static void main(String[] args) throws IOException, XMLStreamException {
		logger.info("Preparing tool");
		final SED2013ImageSimilarityMatrixTool tool = new SED2013ImageSimilarityMatrixTool(args);
		
		File inf = new File(tool.input);
		final SparseMatrix imageGraph = new SparseMatrix(tool.nIndexes(), tool.nIndexes());
		for (int i = 0; i < imageGraph.rowCount(); i++) {
			imageGraph.put(i, i, 1);
		}
		
		logger.info("Loading similarities from: " + inf.getAbsolutePath());
		tool.new ImageGraphStream(inf).forEach(new Operation<ImageGraphEdge>() {
			int read = 0;
			@Override
			public void perform(ImageGraphEdge object) {
				LoggerUtils.debug(logger, String.format("Read Lines: %d",read), read++ % 500 == 0);
				long r1 = tool.indexMap.get(object.img1).first;
				long r2 = tool.indexMap.get(object.img2).first;
				
				double normalise = tool.nmOp.normalise(object.value);
				imageGraph.put((int)r1, (int)r2, normalise);
				imageGraph.put((int)r2, (int)r1, normalise);
			}
		});
		String outname = String.format("%s",inf.getName() + ".mat");
		File outroot = new File(String.format("%s/%s",tool.output , tool.nmOp.toString()));
		outroot.mkdirs();
		File out = new File(outroot,outname);
		logger.info("Saving sparse matrix: " + out.getAbsolutePath());
		IOUtils.writeToFile(imageGraph, out);		
	}

	private double findMaxValue() throws IOException {
		final double[] max = new double[]{-Double.MAX_VALUE};
		new ImageGraphStream(new File(this.input)).forEach(new Operation<ImageGraphEdge>() {
			@Override
			public void perform(ImageGraphEdge object) {
				max[0] = Math.max(max[0], object.value);
			}
		});
		return max[0];
	}

	private int nIndexes() {
		return nIndexes;
	}
}
