package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.TrieDateField;
import org.openimaj.mediaeval.data.CursorWrapperPhoto;
import org.openimaj.mediaeval.data.XMLCursorStream;
import org.openimaj.mediaeval.evaluation.datasets.SED2013ExpOne;
import org.openimaj.util.function.Operation;
import org.openimaj.util.function.Predicate;
import org.openimaj.util.stream.Stream;

import com.aetrion.flickr.photos.GeoData;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.tags.Tag;

/**
 * @author Jonathan Hare (jsh2@ecs.soton.ac.uk), Sina Samangooei (ss@ecs.soton.ac.uk), David Duplaw (dpd@ecs.soton.ac.uk)
 *
 */
public class SED2013SolrIndexCreator {
	/** Logging */
	private static Logger log = Logger.getLogger(SED2013SolrIndexCreator.class);

	/** Some constant counters */
	private static int BATCH_SIZE = 20000;

	/** Solr file names */
	private static String SOLR_CONFIG = "solrconfig.xml";
	private static String SOLR_SCHEMA = "schema.xml";

	interface PhotoDocumentAppender {
		public void addFieldToDoc(Photo p, SolrInputDocument doc);
	}

	private ArrayList<PhotoDocumentAppender> columns;
	private void prepareAppenders(){
		columns = new ArrayList<PhotoDocumentAppender>();
		columns.add(new PhotoDocumentAppender() {
			@Override
			public void addFieldToDoc(Photo p, SolrInputDocument doc) {
				doc.addField("id", p.getId());
			}
		});
		columns.add(new PhotoDocumentAppender() {
			@Override
			public void addFieldToDoc(Photo p, SolrInputDocument doc) {
				doc.addField("url", p.getUrl());
			}
		});
		columns.add(new PhotoDocumentAppender() {
			@Override
			public void addFieldToDoc(Photo p, SolrInputDocument doc) {
				doc.addField("title", p.getTitle());
			}
		});
		columns.add(new PhotoDocumentAppender() {
			@Override
			public void addFieldToDoc(Photo p, SolrInputDocument doc) {
				for (Object otag : p.getTags()) {
					Tag t = (Tag)otag;
					doc.addField("tag", t.getValue());
				}
			}
		});
		columns.add(new PhotoDocumentAppender() {
			@Override
			public void addFieldToDoc(Photo p, SolrInputDocument doc) {
				 doc.addField("description", p.getDescription());
			}
		});
		columns.add(new PhotoDocumentAppender() {
			@Override
			public void addFieldToDoc(Photo p, SolrInputDocument doc) {
				if(p.getDatePosted()!=null)
					doc.addField("timeposted", TrieDateField.formatExternal(p.getDatePosted()));
			}
		});
		columns.add(new PhotoDocumentAppender() {
			@Override
			public void addFieldToDoc(Photo p, SolrInputDocument doc) {
				if(p.getDateTaken()!=null)
					doc.addField("timetaken", TrieDateField.formatExternal(p.getDateTaken()));
			}
		});
		columns.add(new PhotoDocumentAppender() {
			@Override
			public void addFieldToDoc(Photo p, SolrInputDocument doc) {
				if(p.hasGeoData()){
					GeoData gd = p.getGeoData();
					doc.addField("location", String.format("%2.4f,%2.4f",gd.getLatitude(),gd.getLongitude()));
				}
			}
		});
		columns.add(new PhotoDocumentAppender() {
			int count = 0;
			@Override
			public void addFieldToDoc(Photo p, SolrInputDocument doc) {
				doc.addField("index",count++);
			}
		});
		if(this.csv!=null){
			columns.add(new PhotoDocumentAppender() {
				@Override
				public void addFieldToDoc(Photo p, SolrInputDocument doc) {
					doc.addField("cluster",csv.get(p.getId()));
				}
			});
		}
	}

	/** Solr index */
	private SolrCore solrCore;
	private CoreContainer solrContainer;
	private EmbeddedSolrServer solrServer;

	private Stream<Photo> photoStream;

	private Map<String, Integer> csv;

	/**
	 * Basic constructor. Instantiate our reader and Solr.
	 *
	 * @param sourceFile
	 *            The input file to read
	 * @param csv
	 * @throws Exception
	 *             if any errors occur
	 */
	public SED2013SolrIndexCreator(File sourceFile, File csv) throws Exception {

		// Load the CSV groundtruth clusters if it exists
		if(csv != null){
			this.csv = SED2013ExpOne.photoClusters(csv);
		}
		this.photoStream = new XMLCursorStream(sourceFile,"photo")
		.map(new CursorWrapperPhoto());

		// Time to bring Solr online
		// Find the Solr home
		String solrHome = System.getProperty("sed2013.solr.home");
		if (solrHome == null) {
			throw new Exception("No 'sed2013.solr.home' provided!");
		}
		File solrHomeFile = new File(solrHome);
		if (!solrHomeFile.exists()) {
			solrHomeFile.mkdirs();
			File confDir = new File(solrHomeFile, "conf");
			confDir.mkdirs();
			FileUtils.copyInputStreamToFile(
					SED2013SolrIndexCreator.class.getResourceAsStream(SOLR_CONFIG),
					new File(confDir, SOLR_CONFIG)
					);
			FileUtils.copyInputStreamToFile(
					SED2013SolrIndexCreator.class.getResourceAsStream(SOLR_SCHEMA),
					new File(confDir, SOLR_SCHEMA)
					);
		}
		solrServer = startSolr(solrHome);
		prepareAppenders();
	}

	private SED2013SolrIndexCreator() {
		prepareAppenders();
	}

	/**
	 * Start up an embedded Solr server.
	 *
	 * @param home
	 *            The path to the Solr home directory
	 * @return EmbeddedSolrServer: The instantiated server
	 * @throws Exception
	 *             if any errors occur
	 */
	private EmbeddedSolrServer startSolr(String home) throws Exception {
		try {
			SolrConfig solrConfig = new SolrConfig(home, SOLR_CONFIG, null);
			IndexSchema schema = new IndexSchema(solrConfig, SOLR_SCHEMA, null);

			solrContainer = new CoreContainer(new SolrResourceLoader(
					SolrResourceLoader.locateSolrHome()));
			CoreDescriptor descriptor = new CoreDescriptor(solrContainer, "",
					solrConfig.getResourceLoader().getInstanceDir());
			descriptor.setConfigName(solrConfig.getResourceName());
			descriptor.setSchemaName(schema.getResourceName());

			solrCore = new SolrCore(null, solrConfig.getDataDir(),
					solrConfig, schema, descriptor);
			solrContainer.register("cheese", solrCore, false);
			// CoreAdminRequest.create
			return new EmbeddedSolrServer(solrContainer, "cheese");
		} catch (Exception ex) {
			log.error("\nFailed to start Solr server\n");
			throw ex;
		}
	}

	/**
	 * Force a commit against the underlying Solr database.
	 *
	 */
	private void commit() {
		try {
			solrServer.commit();
		} catch (Exception ex) {
			log.error("Failed to commit: ", ex);
		}
	}

	/**
	 * Force an optimize call against the underlying Solr database.
	 *
	 */
	private void optimize() {
		try {
			solrServer.optimize();
		} catch (Exception ex) {
			log.error("Failed to commit: ", ex);
		}
	}

	/**
	 * Main processing loop for the function
	 *
	 * @param counter
	 *            The number of rows to execute during this loop
	 * @return int: The number of rows read this pass
	 * @throws Exception
	 *             if any errors occur
	 */
	public int loop(final int counter) throws Exception {
		final int seen[] = new int[1];
		this.photoStream.forEach(new Operation<Photo>() {

			@Override
			public void perform(Photo object) {
				SED2013SolrIndexCreator.this.process(object);

			}
		}, new Predicate<Photo>() {
			@Override
			public boolean test(Photo object) {
				if(seen[0]%1000 == 0){
					log.debug("Number of document seen this batch: " + seen[0]);
				}
				return ++seen[0]>=counter;
			}
		});

		return seen[0];
	}

	/**
	 * Process the photo
	 *
	 * @param p photo
	 */
	private void process(Photo p) {
		try {
			solrServer.add(createSolrDoc(p,this));
		} catch (Exception ex) {
			log.error("Failed to add document:");
			log.error("Stack trace: ", ex);
		}
	}

	/**
	 * Create a Solr document from the provided Geonames column data.
	 * @param p the photo object
	 *
	 * @return SolrInputDocument: The prepared document
	 */
	public static SolrInputDocument createSolrDoc(Photo p) {
		return createSolrDoc(p,new SED2013SolrIndexCreator());
	}
	
	/**
	 * Create a Solr document from the provided Geonames column data.
	 * @param p the photo object
	 *
	 * @return SolrInputDocument: The prepared document
	 */
	public static SolrInputDocument createSolrDoc(Photo p, SED2013SolrIndexCreator ret ) {
		
		SolrInputDocument doc = new SolrInputDocument();
		for (PhotoDocumentAppender key : ret.columns) {
			key.addFieldToDoc(p, doc);
		}
		return doc;
	}

	/**
	 * Shutdown function for cleaning up instantiated object.
	 *
	 */
	public void shutdown() {
		if (solrContainer != null) {
			solrContainer.shutdown();
		}
	}

	/**
	 * Command line entry point.
	 *
	 * @param args
	 *            Array of String parameters from the command line
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// Make we were given an appropriate parameter
		if (args.length < 1) {
			log.error("ERROR: Usage requires xml file!");
			return;
		}

		// Validate it
		File file = new File(args[0]);
		if (file == null || !file.exists()) {
			log.error("ERROR: The input file does not exist!");
			return;
		}
		File csv = null;
		if(args.length > 1){
			csv = new File(args[1]);
			if(csv == null || !csv.exists()){
				log.error("ERROR: CSV ground truth provided does not exist!");
				return;
			}
		}

		// Get ready to harvest
		SED2013SolrIndexCreator harvester = null;
		try {
			harvester = new SED2013SolrIndexCreator(file,csv);
		} catch (Exception ex) {
			// A reason for death was logged in the constructor
			log.error("Stack trace: ", ex);
		}

		log.debug("\n\n===================\n\n");

		int count = 0;

		// Run a single batch
		try {
			while(true) {
				int read = harvester.loop(BATCH_SIZE);
				count += read;
				log.info("Rows read: " + count);

				// Commit after each batch
				try {
					harvester.commit();
				} catch (Exception ex) {
					log.info("Commit failed");
					log.error("Stack trace: ", ex);
				}

				// Did we finish?
				if (read < BATCH_SIZE) {
					break;
				}
			}
		} catch (Exception ex) {
			log.error("ERROR: An error occurred in the processing loop: ", ex);
		}

		try {
			harvester.commit();
			log.info("Index optimize...");
			harvester.optimize();
			log.info("... completed");
		} catch (Exception ex) {
			log.info("... failed");
			log.error("Stack trace: ", ex);
		}
		log.info("\n\n===================\n\n");

		harvester.shutdown();
	}
}
