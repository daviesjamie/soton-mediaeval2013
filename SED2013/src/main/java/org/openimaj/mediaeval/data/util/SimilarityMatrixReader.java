package org.openimaj.mediaeval.data.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.openimaj.io.IOUtils;
import org.openimaj.mediaeval.evaluation.solr.SimilarityMatrixWrapper;

import ch.akuhn.matrix.SparseMatrix;
import ch.akuhn.matrix.Vector;

/**
 * A similarity matrix reader that supports reading specific sizes of matrix as well
 * as caching matricies
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SimilarityMatrixReader {
	
	private File cache;
	private int rowstart;
	private int rowend ;
	private String root;
	/**
	 * 
	 */
	private SimilarityMatrixReader(String root) {
		this.root = root;
		this.cache = null;
		rowstart = 0;
		rowend = -1;
	}
	
	private SimilarityMatrixReader(String root,String cache, int rowstart, int rowend) {
		this.root = root;
		if(cache != null)
		{
			this.cache = new File(cache);
			this.cache.mkdirs();
		}
		this.rowstart = rowstart;
		this.rowend = rowend;
	}

	/**
	 * @param root
	 * @param start
	 * @param end
	 */
	private SimilarityMatrixReader(String root, int start, int end) {
		this.root = root;
		this.rowstart = start;
		this.rowend = end;
	}

	/**
	 * @param root
	 * @param desiredMatricies
	 * @return the matricies found,
	 * @throws IOException
	 */
	public static Map<String,SimilarityMatrixWrapper> readSparseMatricies(String root, String ... desiredMatricies) throws IOException {
		return new SimilarityMatrixReader(root).internalReadSparseMatricies(desiredMatricies);
	}
	
	/**
	 * @param start 
	 * @param end 
	 * @param root
	 * @param desiredMatricies
	 * @return the matricies found,
	 * @throws IOException
	 */
	public static Map<String,SimilarityMatrixWrapper> readSparseMatricies(int start, int end, String root, String ... desiredMatricies) throws IOException {
		return new SimilarityMatrixReader(root,start,end).internalReadSparseMatricies(desiredMatricies);
	}
	
	/**
	 * @param cache the location to save matricies loaded
	 * @param rowstart 
	 * @param rowend 
	 * @param root
	 * @param desiredMatricies
	 * @return the matricies found,
	 * @throws IOException
	 */
	public static Map<String,SimilarityMatrixWrapper> readCachedSparseMatricies(String cache, int rowstart, int rowend, String root, String ... desiredMatricies) throws IOException {
		return new SimilarityMatrixReader(root,cache,rowstart, rowend).internalReadSparseMatricies(desiredMatricies);
	}
	
	final Logger logger = Logger.getLogger(SimilarityMatrixReader.class);
	/**
	 * Read the matricies (either in parts or as a whole) from the root location
	 * @param root
	 * @param desiredMatricies the matricies to look for
	 * @return all matricies in the location
	 * @throws IOException
	 */
	private Map<String,SimilarityMatrixWrapper> internalReadSparseMatricies(String ... desiredMatricies) throws IOException {
		File rootf = null;
		Map<String, SimilarityMatrixWrapper> matricies = new HashMap<String, SimilarityMatrixWrapper>();
		if(root!=null){			
			rootf = new File(root);
			if(!rootf.isDirectory()){
				return readFromMats(new File[]{rootf});
			}
			// The root is not null, AND a directory! let's begin
			// Find all the part files in the root
			File[] parts = rootf.listFiles(partFilter());
			if(parts == null) throw new IOException("No such file: " + root);
			if(parts.length != 0) matricies = readMultiPart(parts,desiredMatricies);
			// Done! Now add all the MAT files in the root
			File[] mats = rootf.listFiles(matfilter(desiredMatricies));
			if(mats.length != 0) matricies.putAll(readFromMats(mats));
		}
		
		// ok cool... are there any desired files that are NOT in the matrix yet? hmm. Try loading them as raw files?
		List<File> desFilesNotAddedYet = new ArrayList<File>();
		for (String des : desiredMatricies) {
			File desfile = new File(des);
			String desname = desfile.getName();
			if(!matricies.containsKey(desname)){
				desFilesNotAddedYet.add(desfile);
			}
		}
		matricies.putAll(readFromMats(desFilesNotAddedYet.toArray(new File[desFilesNotAddedYet.size()])));
		
		return matricies;
	}

	private FilenameFilter partFilter() {
		return new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("part");
			}
		};
	}
	

	private FilenameFilter matfilter(final String[] desiredMatricies) {
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
	private Map<String, SimilarityMatrixWrapper> readFromMats(File ... mats) throws IOException {
		return readFromMats(mats,false);
	}
	private Map<String, SimilarityMatrixWrapper> readFromMats(File[] mats, boolean nocache) throws IOException {
		Map<String, SimilarityMatrixWrapper> matMap = new HashMap<String, SimilarityMatrixWrapper>();
		
		
		for (File matf : mats) {
			SimilarityMatrixWrapper mat = null;
			if(cache!=null && !nocache){
				String cacheName = generateCacheName(matf.getAbsolutePath(),rowstart,rowend);
				File cacheFile = new File(cache,cacheName);
				if(cacheFile.exists()){
					logger.debug("Reading from cache: " + matf);
					mat = IOUtils.readFromFile(cacheFile);
				}
				else{
					logger.debug("Reading from file: " + matf);
					SparseMatrix spmat = IOUtils.readFromFile(matf);
					mat = new SimilarityMatrixWrapper(spmat,rowstart,rowend);
					logger.debug("Writing to cache: " + matf);
					IOUtils.writeToFile(mat, cacheFile);
				}
			}
			else{				
				logger.debug("Reading: " + matf);
				SparseMatrix spmat = IOUtils.readFromFile(matf);
				mat = new SimilarityMatrixWrapper(spmat, rowstart,rowend);
			}
			matMap.put(matf.getName(), mat);
			
		}
		logger.debug("Done reading sparse matricies.");
		return matMap ;
	}

	/**
	 * @param name
	 * @param rowstart 
	 * @param rowend
	 * @return the cache name of a given file with sub rows and cols
	 */
	public static String generateCacheName(String name, int rowstart, int rowend){
		name += String.format("%d %d",rowstart,rowend);
		return DigestUtils.md5Hex(name) + ".mat";
	}

	private Map<String, SimilarityMatrixWrapper> readMultiPart(File[] parts, String ... desiredMatricies) throws IOException {
		Map<String, SimilarityMatrixWrapper> combined = null;
		combined = new HashMap<String, SimilarityMatrixWrapper>();
		
		// Check the cache on the first loop for every file in the first part
		// The desiredMatricies is then used to select all files in the first part but not combined in the cache
		if(this.cache!=null){
			File[] mats = parts[0].listFiles(matfilter(desiredMatricies));
			List<String> newDesiredMats = new ArrayList<String>();
			for (File file : mats) {
				String desiredFile = file.getName();
				String cacheName = generateCacheName(new File(this.root).getAbsolutePath() + "#" + desiredFile, rowstart, rowend);
				File cacheFile = new File(this.cache,cacheName);
				if(cacheFile.exists()){
					logger.debug("Reading from cache: " + desiredFile);
					combined.put(desiredFile, (SimilarityMatrixWrapper) IOUtils.readFromFile(cacheFile));
				}
				else{
					logger.debug("Combining from files: " + desiredFile);
					newDesiredMats.add(desiredFile);
				}
			}
			desiredMatricies = newDesiredMats.toArray(new String[newDesiredMats.size()]);
		}
		// in the caching case, if the desiredMatricies is currently length 0 we know we have all the matricies we need! so don't go into the main combine loop
		if(this.cache!=null && desiredMatricies.length == 0){
			// We have no work left to do, everything was loaded from the cache by definintion! 
		}
		else{
			for (File part : parts) {
				File[] mats = part.listFiles(matfilter(desiredMatricies));
				Map<String, SimilarityMatrixWrapper> newpart = readFromMats(mats,true);
				
				for (Entry<String, SimilarityMatrixWrapper> mat : newpart.entrySet()) {
					if(!combined.containsKey(mat.getKey())){
						combined.put(mat.getKey(), mat.getValue());
					}
					else{
						SimilarityMatrixWrapper current = combined.get(mat.getKey());
						SimilarityMatrixWrapper newsparse = mat.getValue();
						logger.debug("Combining sparse matrix: " + mat.getKey());
						int r = 0;
						for (Vector newsparserow : newsparse.matrix().rows()) {
							for (ch.akuhn.matrix.Vector.Entry ent : newsparserow.entries()) {							
								current.matrix().put(r, ent.index, ent.value);
							}
							r++;
						}
						logger.debug("Done! new density: " + (double)current.matrix().used()/((double)current.matrix().rowCount() * (double)current.matrix().columnCount()));
					}
				}
			}
		}
		if(this.cache!=null){
			// Desired matricies MUST be not null, but it might be empty. If it is empty all files were loaded from cache, don't save them again
			for (String des : desiredMatricies) {
				String cacheName = generateCacheName(new File(this.root).getAbsolutePath() + "#" + des, rowstart, rowend);
				File cacheFile = new File(this.cache,cacheName);
				IOUtils.writeToFile(combined.get(des),cacheFile);
			}
		}
		return combined;
	}
	
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String root = "/home/ss/Experiments/mediaeval/SED2013/training.sed2013.solr.sparsematrix/solrsort=true/solrtotal=200/solreps=0.40/inc=10000/";
		String cache = "/home/ss/Experiments/mediaeval/SED2013/.testcache";
		Map<String, SimilarityMatrixWrapper> found ;
//		found = readSparseMatricies(root,"aggregationMean.mat");
//		System.out.println(found.keySet());
		flushCache(cache);
		found = readCachedSparseMatricies(cache,0,100,root,"aggregationMean.mat");
		System.out.println(found.keySet());
		found = readCachedSparseMatricies(cache,0,100,root,"aggregationMean.mat");
		System.out.println(found.keySet());
		found = readCachedSparseMatricies(cache,30187,30287,root,"aggregationMean.mat");
		System.out.println(found.keySet());
		found = readCachedSparseMatricies(cache,30187,30287,root,"aggregationMean.mat");
		System.out.println(found.keySet());
	}

	private static void flushCache(String cache) {
		File[] todel = new File(cache).listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("mat");
			}
		});
		for (File file : todel) {
			file.delete();
		}
	}
}
