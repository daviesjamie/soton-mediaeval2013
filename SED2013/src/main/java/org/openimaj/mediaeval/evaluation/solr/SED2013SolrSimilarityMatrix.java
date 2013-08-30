package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.openimaj.feature.FeatureVector;
import org.openimaj.io.IOUtils;
import org.openimaj.mediaeval.data.SolrDocumentToIndexedPhoto;
import org.openimaj.mediaeval.data.SolrStream;
import org.openimaj.mediaeval.evaluation.datasets.PPK2012ExtractCompare;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.mediaeval.feature.extractor.CombinedFVComparator;
import org.openimaj.mediaeval.feature.extractor.CombinedFVComparator.Mean;
import org.openimaj.mediaeval.feature.extractor.DatasetSimilarity.ExtractorComparator;
import org.openimaj.util.function.Operation;
import org.openimaj.util.pair.DoubleObjectPair;

import ch.akuhn.matrix.SparseMatrix;
import ch.akuhn.matrix.SparseVector;
import ch.akuhn.matrix.Vector;

import com.aetrion.flickr.photos.Photo;

/**
 * @author Jonathan Hare (jsh2@ecs.soton.ac.uk), Sina Samangooei (ss@ecs.soton.ac.uk), David Duplaw (dpd@ecs.soton.ac.uk)
 *
 */
public class SED2013SolrSimilarityMatrix {
	private static final class DoublePhotoPairComparator implements Comparator<DoubleObjectPair<IndexedPhoto>> {
		@Override
		public int compare(DoubleObjectPair<IndexedPhoto> o1, DoubleObjectPair<IndexedPhoto> o2) {
			return ((Long)o1.second.first).compareTo(o2.second.first);
		}
	}
	private SED2013Index index;
	private int collectionN;
	private List<ExtractorComparator<Photo, ? extends FeatureVector>> fe;
	private SolrStream solrStream;
	
	public SED2013SolrSimilarityMatrix(String tfidfLocation, String featureCacheLocation) throws IOException {
		this(tfidfLocation, featureCacheLocation, null);
	}
	
	/**
	 * @param tfidfLocation
	 * @param featureCacheLocation
	 * @throws IOException
	 */
	public SED2013SolrSimilarityMatrix(String tfidfLocation, String featureCacheLocation, String indexURL) throws IOException {
		if(indexURL == null)
			this.index = SED2013Index.instance();
		else
			this.index = SED2013Index.instance(indexURL);
		SolrQuery q = new SolrQuery("*:*");
		q.setSortField("index", ORDER.asc);
		this.solrStream = new SolrStream(q, index.getSolrIndex());
		this.collectionN = solrStream.getNumResults();
//		this.fe = PPK2012ExtractCompare.similarity(tfidfLocation, featureCacheLocation);
		this.fe = PPK2012ExtractCompare.similarityLogGeo(tfidfLocation, featureCacheLocation);
	}
	
	/**
	 * @return create a sparse matrix for each comparator
	 */
	public Map<String, SparseMatrix> createSimilarityMatricies(){
		final Map<String, SparseMatrix> allSimMat = new HashMap<String, SparseMatrix>();
		solrStream
		.map(new SolrDocumentToIndexedPhoto())
		.forEach(new Operation<IndexedPhoto>() {

			@Override
			public void perform(IndexedPhoto p) {
				QueryResponse res;
				try {
					SolrDocumentList results = performSolrQuery(p);
					final Mean<Photo> comp = new CombinedFVComparator.Mean<Photo>(fe) ;
					
					Map<String, SparseMatrix> rowmat = buildComparatorSparseRow(p, results, comp);
					updateSimilarities(allSimMat, p, rowmat);
				} catch (Exception e) {
					logger.error("Error querying for photo: " + p.second.getId(),e);
				}

			}
		});
		return allSimMat;
	}
	
	private SolrDocumentList performSolrQuery(IndexedPhoto p) throws Exception {
		SolrDocumentList results; 
		if(this.searchcache == null){			
			QueryResponse res;
			res = index.query(p.second, solrQueryN,!deactivateSort);
			results = res.getResults();
		}
		else{
			final String searchCacheRoot = String.format("%s/solrsort=%s/solrtotal=%d",this.searchcache,!deactivateSort,solrQueryN);
			File scr = new File(searchCacheRoot);
			scr.mkdirs();
			File cacheFile = new File(scr,p.first + ".search");
			if(!cacheFile.exists()){
				QueryResponse res;
				res = index.query(p.second, solrQueryN,!deactivateSort);
				results = res.getResults();
				IOUtils.writeToFile(results, cacheFile);
			}
			else{
				results = IOUtils.readFromFile(cacheFile);
			}
		}
		return results;
	}
	
	private static Logger logger = Logger.getLogger(SED2013SolrSimilarityMatrix.class);

	
	double eps = 0.4; // used to be 0.4
	int solrQueryN = 200; // used to be 200
	public boolean deactivateSort = false;
	public String searchcache = null;
	/**
	 * @param expRoot the root to save the mats
	 * @param mats the mats to save
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws FileNotFoundException
	 */
	public void write(String expRoot, Map<String,SparseMatrix> mats) throws IOException, XMLStreamException, FileNotFoundException {
		final String matRoot = String.format("%s",expRoot);
		File rootFile = new File(matRoot);
		rootFile.mkdirs();
		for (Entry<String, SparseMatrix> ent : mats.entrySet()) {
			File outMat = new File(matRoot,ent.getKey() + ".mat");
			IOUtils.writeToFile(ent.getValue(), outMat);
		}
	}
	
	/**
	 * @param expRoot the root to save the mats
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws FileNotFoundException
	 */
	public void write(String expRoot) throws IOException, XMLStreamException, FileNotFoundException {
		this.write(expRoot,this.createSimilarityMatricies());
	}
	
	private Map<String,SparseMatrix> buildComparatorSparseRow(IndexedPhoto p, List<SolrDocument> results, CombinedFVComparator<Photo> comp) {

		Map<String,TreeSet<DoubleObjectPair<IndexedPhoto>>> treemap = new HashMap<String,TreeSet<DoubleObjectPair<IndexedPhoto>>>();

		for (SolrDocument photoIndex : results) {
			IndexedPhoto ip = IndexedPhoto.fromDoc(photoIndex);
			
			Map<String, Double> compare = comp.compareAggregation(p.second, ip.second);
//			
			for (Entry<String, Double> comparatorScore : compare.entrySet()) {
				String comparatorKey = comparatorScore.getKey();
				String comparatorKeyThresh = "thresholded_" + comparatorKey ;
				TreeSet<DoubleObjectPair<IndexedPhoto>> tree = treemap.get(comparatorKey);
				TreeSet<DoubleObjectPair<IndexedPhoto>> treeThresh = treemap.get(comparatorKeyThresh);
				if(tree == null) {
					treemap.put(comparatorKey, tree = new TreeSet<DoubleObjectPair<IndexedPhoto>>(new DoublePhotoPairComparator()));
					treemap.put(comparatorKeyThresh, treeThresh = new TreeSet<DoubleObjectPair<IndexedPhoto>>(new DoublePhotoPairComparator()));
					
				}
				tree.add(DoubleObjectPair.pair(comparatorScore.getValue(), ip));
				if(comparatorScore.getValue() > eps){
					treeThresh.add(DoubleObjectPair.pair(comparatorScore.getValue(), ip));
				}

			}
		}
		Map<String, SparseMatrix> rowmats = new HashMap<String,SparseMatrix>();
		for (Entry<String, TreeSet<DoubleObjectPair<IndexedPhoto>>> nametree: treemap.entrySet()) {
			TreeSet<DoubleObjectPair<IndexedPhoto>> tree = nametree.getValue();
			SparseMatrix rowmat = new SparseMatrix(1, this.collectionN);
			for (DoubleObjectPair<IndexedPhoto> pair : tree) {
				
				rowmat.put(0, (int) pair.second.first, pair.first);
			}
			rowmats.put(nametree.getKey(), rowmat);
		}
		return rowmats;
	}

	/**
	 * @param expRoot
	 * @param incBuild
	 */
	public void createAndWrite(String expRoot, final int incBuild) {
		final String matRoot = String.format("%s/solrsort=%s/solrtotal=%d/solreps=%2.2f/inc=%d",expRoot,!deactivateSort,solrQueryN,eps,incBuild);
		final File rootFile = new File(matRoot);
		rootFile.mkdirs();
		final Map<String, SparseMatrix> allSimMat = new HashMap<String, SparseMatrix>();
		
		final int[] totalWritten = new int []{0};
		final int[] currentSeen = new int[]{0};
		solrStream
		.map(new SolrDocumentToIndexedPhoto())
		.forEach(new Operation<IndexedPhoto>() {
			@Override
			public void perform(IndexedPhoto p) {
				File outPath = new File(rootFile,"part_" + totalWritten[0]);
				if(!outPath.exists()){
					try {
						SolrDocumentList results = performSolrQuery(p);
						final Mean<Photo> comp = new CombinedFVComparator.Mean<Photo>(fe) ;
						Map<String, SparseMatrix> rowmat = buildComparatorSparseRow(p, results, comp);
						if(p.first % 1000==0){
							logger.info("Reading in row: " + p.first);
						}
						updateSimilarities(allSimMat, p, rowmat);
					} catch (Exception e) {
						logger.error("Error querying for photo: " + p.second.getId(),e);
					}
				}
				else{
					if(currentSeen[0] == 0) logger.info(String.format("Part %d already exists, skipping!",totalWritten[0]));
				}
				currentSeen[0]++;
				
				if(currentSeen[0] > incBuild){
					flushSimMat(allSimMat, totalWritten[0], currentSeen[0], outPath);
					totalWritten[0]++;
					currentSeen[0] = 0;
				}
			}
			
		});
		if(currentSeen[0] > 0){
			File outPath = new File(rootFile,"part_" + totalWritten[0]);
			flushSimMat(allSimMat, totalWritten[0], currentSeen[0], outPath);
			totalWritten[0]++;
			currentSeen[0] = 0;
		}
	}
	
	private void flushSimMat(final Map<String, SparseMatrix> allSimMat, final int totalWritten, final int currentSeen, File outPath) {
		if(!outPath.exists()){
			logger.info("Reached inc! Writing: " + outPath);
			try {
				write(outPath.getAbsolutePath(), allSimMat);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			HashSet<String> compKeySet = new HashSet<String>(allSimMat.keySet());
			for (String compKey : compKeySet) {
				allSimMat.put(compKey, new SparseMatrix(collectionN,collectionN));
			}
		}
	}
	
	/**
	 * Read the matricies (either in parts or as a whole) from the root location
	 * @param root
	 * @return all matricies in the location
	 * @throws IOException
	 */
	public static Map<String,SparseMatrix> readSparseMatricies(String root, String ... desiredMatricies) throws IOException {
		File[] parts = new File(root).listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("part");
			}
		});
		if(parts == null) throw new IOException("No such file: " + root);
		if(parts.length != 0) return readMultiPart(parts,desiredMatricies);
		File[] mats = new File(root).listFiles(matfilter(desiredMatricies));
		return readFromMats(mats);
	}
	
	private void updateSimilarities(final Map<String, SparseMatrix> seenSimMat,IndexedPhoto p, Map<String, SparseMatrix> newSimMat) {
		for (Entry<String, SparseMatrix> namerow : newSimMat.entrySet()) {
			String comparator = namerow.getKey();
			SparseMatrix comprow = namerow.getValue();
			if(!seenSimMat.containsKey(comparator)){
				seenSimMat.put(comparator, new SparseMatrix(collectionN,collectionN));
				
			}
			SparseMatrix toAlter = seenSimMat.get(comparator);
			Vector newrow = comprow.row(0);
			
			for (ch.akuhn.matrix.Vector.Entry rowent : newrow.entries()) {
				if(Double.isNaN(rowent.value)) continue; // we leave out NaN
				toAlter.put((int) p.first, rowent.index, rowent.value);
				toAlter.put(rowent.index,(int) p.first, rowent.value);
			}
		}
	}

	private static FilenameFilter matfilter(final String[] desiredMatricies) {
		return new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if(!name.endsWith("mat")) return false;
				if(desiredMatricies.length != 0)
				{
					for (String string : desiredMatricies) {
						if(name.matches(string)) return true;
					}
					return false;
				}
				return true;
			}
		};
	}

	private static Map<String, SparseMatrix> readFromMats(File[] mats) throws IOException {
		Map<String, SparseMatrix> matMap = new HashMap<String, SparseMatrix>();
		for (File matf : mats) {
			logger.debug("Reading: " + matf);
			matMap.put(matf.getName(), (SparseMatrix) IOUtils.readFromFile(matf));
			
		}
		logger.debug("Done reading sparse matricies.");
		return matMap ;
	}

	private static Map<String, SparseMatrix> readMultiPart(File[] parts, String ... desiredMatricies) throws IOException {
		Map<String, SparseMatrix> combined = null;
		for (File part : parts) {
			File[] mats = part.listFiles(matfilter(desiredMatricies));
			Map<String, SparseMatrix> newpart = readFromMats(mats);
			if(combined == null){
				combined = newpart;
			}
			else{
				for (Entry<String, SparseMatrix> mat : combined.entrySet()) {
					SparseMatrix newsparse = newpart.get(mat.getKey());
					logger.debug("Combining sparse matrix: " + mat.getKey());
					int r = 0;
					for (Vector file : newsparse.rows()) {
						for (ch.akuhn.matrix.Vector.Entry ent : file.entries()) {							
							mat.getValue().put(r, ent.index, ent.value);
						}
						r++;
					}
					logger.debug("Done! new density: " + (double)mat.getValue().used()/((double)mat.getValue().rowCount() * (double)mat.getValue().columnCount()));
				}
			}
		}
		return combined;
	}
	
	public static void main(String[] args) throws IOException {
		Map<String, SparseMatrix> found = readSparseMatricies("/home/ss/Experiments/mediaeval/SED2013/training.sed2013.solr.sparsematrix/solrtotal=200/solreps=0.40/inc=10000/","aggregationMean");
		System.out.println(found.keySet());
	}
	
}
