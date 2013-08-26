package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.schema.IndexSchema;
import org.openimaj.data.identity.Identifiable;
import org.openimaj.feature.FeatureVector;
import org.openimaj.mediaeval.data.util.PhotoUtils;
import org.openimaj.mediaeval.evaluation.datasets.PPK2012ExtractCompare;
import org.openimaj.mediaeval.feature.extractor.CombinedFVComparator;
import org.openimaj.mediaeval.feature.extractor.CombinedFVComparator.Mean;
import org.openimaj.mediaeval.feature.extractor.DatasetSimilarity.ExtractorComparator;
import org.openimaj.time.Timer;
import org.openimaj.util.pair.LongObjectPair;
import org.xml.sax.SAXException;

import com.aetrion.flickr.photos.Photo;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk), Jonathan Hare
 *         (jsh2@ecs.soton.ac.uk), David Duplaw (dpd@ecs.soton.ac.uk)
 *
 */
public class SED2013Index {
	private static Logger logger = Logger.getLogger(SED2013Index.class);
	private static final int N_ROWS = 10;

	private static final String[] DEFAULT_FIELDS = new String[] { "*,score" };

	/** Logging */
	private static Logger log = Logger.getLogger(SED2013Index.class);

	/** Solr file names */
	private static String SOLR_CONFIG = "solrconfig.xml";
	private static String SOLR_SCHEMA = "schema.xml";

	/** Solr index */
	private SolrServer solrIndex;

	private static String[] rqfArray;
	private static String[] afArray;

	private static SED2013Index instance;

	private static Set<String> rawQueryFields;

	private static Set<String> timeFields;

	private static Set<String> locFields;

	private static Set<String> allFields;
	private static HashMap<String, Double> fieldBoost;

	static{
		fieldBoost = new HashMap<String,Double>();
		fieldBoost.put("timeposted", 10d);
		fieldBoost.put("timetaken", 50d);
		fieldBoost.put("title", 0.01d);
		fieldBoost.put("tag", 50d);
		fieldBoost.put("description", 0.01d);
		fieldBoost.put("location", 30d);
		rawQueryFields = new HashSet<String>();
		rawQueryFields.add("title");
		rawQueryFields.add("tag");
		rawQueryFields.add("description");
		rqfArray = rawQueryFields.toArray(new String[rawQueryFields.size()]);

		timeFields = new HashSet<String>();
		timeFields.add("timeposted");
		timeFields.add("timetaken");

		locFields = new HashSet<String>();
		locFields.add("location");

		allFields = new HashSet<String>();
		allFields.addAll(rawQueryFields);
		allFields.addAll(timeFields);
		allFields.addAll(locFields);
		afArray = allFields.toArray(new String[allFields.size()]);
	}

	private SED2013Index() {
		// Find the Solr home
		String solrHome = System.getProperty("sed2013.solr.home");
		if (solrHome == null) {
			log.error("No 'sed2013.solr.home' provided!");
			return;
		}
		// Validate on a basic level
		File solrDir = new File(solrHome);
		if (solrDir == null || !solrDir.exists() || !solrDir.isDirectory()) {
			log.error(String.format("SOLR_HOME does not exist, or is not a directory: '%s'",solrHome));
			return;
		}
		try {
			this.setSolrIndex(buildSolrIndex(solrHome));
		} catch (Exception ex) {
			log.error("\n... Solr failed to load!");
			log.error("Stack trace: ", ex);
			log.error("\n=============");
		}
	}

	private SED2013Index(String url) {
		try {
			this.setSolrIndex(new HttpSolrServer(url));
		} catch (Exception ex) {
			log.error("\n... Solr failed to load!");
			log.error("Stack trace: ", ex);
			log.error("\n=============");
		}
	}

	/**
	 * @return get the {@link SED2013Index} instance
	 */
	public static synchronized SED2013Index instance() {
		if (instance == null) {
			instance = new SED2013Index();
		}
		return instance;
	}


	/**
	 * @return get the {@link SED2013Index} instance
	 */
	public static synchronized SED2013Index instance(String url) {
		if (instance == null) {
			instance = new SED2013Index(url);
		}
		return instance;
	}

	private EmbeddedSolrServer buildSolrIndex(String home) throws ParserConfigurationException, IOException, SAXException {
		SolrConfig solrConfig = new SolrConfig(home, SOLR_CONFIG, null);
		IndexSchema schema = new IndexSchema(solrConfig, SOLR_SCHEMA, null);

		CoreContainer solrContainer = new CoreContainer(new SolrResourceLoader(
				SolrResourceLoader.locateSolrHome()));
		CoreDescriptor descriptor = new CoreDescriptor(solrContainer, "",
				solrConfig.getResourceLoader().getInstanceDir());
		descriptor.setConfigName(solrConfig.getResourceName());
		descriptor.setSchemaName(schema.getResourceName());
		SolrCore solrCore = new SolrCore(null, solrConfig.getDataDir(),solrConfig, schema, descriptor);

		solrContainer.register("cheese", solrCore, false);
		return new EmbeddedSolrServer(solrContainer, "cheese");
	}
	
	/**
	 * @param p
	 * @return
	 * @throws SolrServerException
	 */
	public QueryResponse query(String textquery, int limit) throws SolrServerException{
		return query(textquery, limit, afArray, null,null);
	}
	/**
	 * @param p
	 * @return
	 * @throws SolrServerException
	 */
	public QueryResponse query(Photo p, int limit, boolean activateSort) throws SolrServerException{
		// This will create a document with a meaningless index, ignore it
		SolrInputDocument solrDoc = SED2013SolrIndexCreator.createSolrDoc(p);
		String textquery = "";
		String filter = null;
		String geosort = null;
		String geoquery = null;
		List<String> timesort = new ArrayList<String>();
		List<String> timequery = new ArrayList<String>();


		for (SolrInputField solrInputField : solrDoc) {
			String name = solrInputField.getName();
			if(rawQueryFields.contains(name)){
				for (Object object : solrInputField) {
					textquery += String.format("%s:\"%s\"^%2.5f",name,ClientUtils.escapeQueryChars(object.toString()),fieldBoost.get(name)) + " ";
				}
			}
			else if(timeFields.contains(name)){
				String time = (String) solrInputField.getValue();
				String timewindow = "30DAY";
				timequery.add(String.format("%s:[ %s-%s TO %s+%s ]^%2.5f",name,time,timewindow,time,timewindow,fieldBoost.get(name)));
				timesort.add(String.format("sub(1,div(log(abs(ms(%s,%s))),%2.5f)) desc",time,name,Math.log(365*24*60*60*1000l)));
			}
			else if(locFields.contains(name)){
				String v = (String) solrInputField.getValue();
				String[] llstrs = v.split(",");
				double lat = Double.parseDouble(llstrs[0]);
				double lon = Double.parseDouble(llstrs[1]);
				geosort = String.format("geodist(location,%2.5f,%2.5f) asc",lat,lon);
				geoquery = String.format("{!geofilt sfield=location pt=%2.5f,%2.5f d=10}^%2.5f",lat,lon,fieldBoost.get(name));
			}
		}
		textquery = textquery.substring(0, textquery.length()-1);
		String sort = null;
		if(activateSort){			
			if(geoquery!=null){
				textquery = join(" ", textquery, join(" ",timequery), geoquery);
				sort = join(", ",join(", ",timesort), geosort,"score desc");
			}
			else{
				textquery = join(" ", textquery, join(" ",timequery));
				sort = join(", ",join(", ",timesort), "score desc");
			}
		}
		return query(textquery, limit, afArray, filter,sort);
	}

	/**
	 * @param query
	 * @param nRows
	 * @param fields
	 * @param filter
	 * @param sort
	 * @return the response
	 * @throws SolrServerException
	 */
	public QueryResponse query(String query, int nRows, String[] fields,String filter,String sort) throws SolrServerException  {
		SolrQuery q = new SolrQuery();
		q.add("defType","edismax");
		q.add("qf", join(" ",rqfArray));
		q.add("lowercaseOperators", "true");
		q.setQuery(query);
		q.setRows(nRows);
		if (filter != null) {
			q.setFilterQueries(filter);
		}
		q.setFacet(false);
		if(sort!=null)
			q.add("sort",sort);
//		logger.debug("Query: " + query);
//		logger.debug("qf: " + join(" ",rqfArray));
//		logger.debug("sort: " + sort);
//		logger.debug("SOLR Query URL:"  + q);
		try{
			return getSolrIndex().query(q);
		} catch(SolrServerException e){
			throw e;
		}
	}

	private String join(String join, Collection<String> rqfArray) {
		return join(join,rqfArray.toArray(new String[rqfArray.size()]));
	}

	private String join(String join, String... rqfArray) {
		String ret = rqfArray[0];
		for (int i = 1; i < rqfArray.length; i++) {
			ret += join + rqfArray[i];
		}
		return ret;
	}

	public static class IndexedPhoto extends LongObjectPair<Photo> implements Identifiable{
		public IndexedPhoto(long index, Photo p) {
			super(index, p);
		}

		public static IndexedPhoto fromDoc(SolrDocument doc){
			long photoIndex = (Long) doc.get("index");
			Photo p = PhotoUtils.createPhoto(doc);
			return new IndexedPhoto(photoIndex, p);
		}

		@Override
		public String getID() {
			return this.second.getId();
		}
	}

	public static void main(String[] args) throws XMLStreamException, SolrServerException, IOException {
		try {
			SED2013Index index = instance("http://localhost:8983/solr/sed2013_train");
			// Some choice experiments
			String bigFile = "/Volumes/data/mediaeval/mediaeval-SED2013/sed2013_dataset_train.xml";
			String expRoot = "/Volumes/data/mediaeval/mediaeval-SED2013/tools/simmat/";
			String tfidf = String.format("/Users/ss/Experiments/sed2013/training.sed2013.photo_tfidf",expRoot);
			String featurecache = String.format("%s/train.all.featurecache",expRoot);
			logger.info(String.format("Loading dataset: %s ", bigFile));
			File xmlFile = new File(bigFile);

//			Stream<Photo> photoStream = new XMLCursorStream(xmlFile,"photo")
//			.map(new CursorWrapperPhoto()).filter(new Predicate<Photo>() {
//				@Override
//				public boolean test(Photo object) {
//					return object.getId().equals("2392913991");
//				}
//			});
//
//			photoStream.hasNext();
//			Photo first = photoStream.next();
			Photo first = index.photoById("3358133370");
			Timer t = Timer.timer();
			QueryResponse res = index.query(first, 100,true);
			int withGeoCode = 0;
			int withoutGeoCode = 0;
			List<ExtractorComparator<Photo, ? extends FeatureVector>> fe = PPK2012ExtractCompare.similarity(tfidf, featurecache);
			final Mean<Photo> comp = new CombinedFVComparator.Mean<Photo>(fe) ;
			for (SolrDocument photoIndex : res.getResults()) {
				IndexedPhoto ip = IndexedPhoto.fromDoc(photoIndex);
//				if(ip.second.hasGeoData()){
//					withGeoCode++;
//				}
//				else{
//					withoutGeoCode++;
//				}
				double compare = comp.compare(first, ip.second);
//				logger.debug(String.format("Id: %s (Taken: %s Posted: %s) Similarity: %s",ip.second.getId(), ip.second.getDateTaken(), ip.second.getDatePosted(),compare));
			}

			logger.debug("With: " + withGeoCode);
			logger.debug("Without: " + withoutGeoCode);
			logger.debug("Took: " + t.duration());

		} finally {
			shutdown();
		}

	}

	private Photo photoById(String string) throws SolrServerException {
		QueryResponse a = query(String.format("id:%s",string), 1, null, null, null);
		return PhotoUtils.createPhoto(a.getResults().get(0));
	}

	public static void shutdown() {
		if(instance!=null)
			instance.getSolrIndex().shutdown();
	}

	public SolrServer getSolrIndex() {
		return solrIndex;
	}

	public void setSolrIndex(SolrServer solrIndex) {
		this.solrIndex = solrIndex;
	}

}
