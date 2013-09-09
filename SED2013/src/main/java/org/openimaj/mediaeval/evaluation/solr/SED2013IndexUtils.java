package org.openimaj.mediaeval.evaluation.solr;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.openimaj.data.dataset.ListBackedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.mediaeval.data.util.PhotoUtils;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;

import com.aetrion.flickr.photos.Photo;

/**
 * Some utils of dealing with a SED2013 lucene index
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class SED2013IndexUtils {
	/**
	 * @param indexFile
	 * @param start
	 * @param end
	 * @return dataset from a solr index
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public static MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto> datasetFromSolr(String indexFile, int start, int end) throws CorruptIndexException, IOException {
//		Query q = NumericRangeQuery.newLongRange("index", 0l, 0l, true, true);
////		final Query q = new QueryParser(Version.LUCENE_40, "tag", new StandardAnalyzer(Version.LUCENE_40)).parse("cheese");
		final Directory directory = new SimpleFSDirectory(new File(indexFile));
		final IndexReader reader = DirectoryReader.open(directory);
		final IndexSearcher searcher = new IndexSearcher(reader);
		MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto> ret = new MapBackedDataset<Integer, ListDataset<IndexedPhoto>, IndexedPhoto>(){
			@Override
			public String toString() {
				return "Clusters: " + this.size() + " Instances: " + this.numInstances();
			}
		};
		for (int i = start; i < end; i++) {
			Query q = NumericRangeQuery.newLongRange("index", (long)i, (long)i, true, true);
			
			TopDocs docs = searcher.search(q, 1);
			ScoreDoc scoreDoc = docs.scoreDocs[0];
			
			final Document d = searcher.doc(scoreDoc.doc);
			Photo p = PhotoUtils.createPhoto(d);
			long index = (Long) d.getField("index").numericValue()-start;
			long cluster = (Long) d.getField("cluster").numericValue();
			
			ListDataset<IndexedPhoto> clusterList = ret.get((int)cluster);
			if(clusterList==null){
				ret.put((int) cluster, clusterList = new ListBackedDataset<IndexedPhoto>());
			}
			clusterList.add(new IndexedPhoto(index, p));
		}
		reader.close();
		return ret ;
	}
	
	/**
	 * @param indexFile
	 * @param start
	 * @param end
	 * @return dataset from a solr index
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public static Map<String, IndexedPhoto> flickrIDIndexedPhoto(String indexFile, int start, int end) throws CorruptIndexException, IOException {
		final Directory directory = new SimpleFSDirectory(new File(indexFile));
		final IndexReader reader = DirectoryReader.open(directory);
		final IndexSearcher searcher = new IndexSearcher(reader);
		Map<String, IndexedPhoto> ret = new HashMap<String, IndexedPhoto>();
		for (int i = start; i < end; i++) {
			Query q = NumericRangeQuery.newLongRange("index", (long)i, (long)i, true, true);
			
			TopDocs docs = searcher.search(q, 1);
			ScoreDoc scoreDoc = docs.scoreDocs[0];
			
			final Document d = searcher.doc(scoreDoc.doc);
			Photo p = PhotoUtils.createPhoto(d);
			long index = (Long) d.getField("index").numericValue()-start;
			
			ret.put(p.getId(),new IndexedPhoto(index, p));
		}
		reader.close();
		return ret ;
	}
	
	/**
	 * @param indexFile
	 * @param start
	 * @param end
	 * @return dataset from a solr index
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public static Map<Integer, IndexedPhoto> indexedPhoto(String indexFile, int start, int end) throws CorruptIndexException, IOException {
		final Directory directory = new SimpleFSDirectory(new File(indexFile));
		final IndexReader reader = DirectoryReader.open(directory);
		final IndexSearcher searcher = new IndexSearcher(reader);
		Map<Integer, IndexedPhoto> ret = new HashMap<Integer, IndexedPhoto>();
		for (int i = start; i < end; i++) {
			Query q = NumericRangeQuery.newLongRange("index", (long)i, (long)i, true, true);
			
			TopDocs docs = searcher.search(q, 1);
			ScoreDoc scoreDoc = docs.scoreDocs[0];
			
			final Document d = searcher.doc(scoreDoc.doc);
			Photo p = PhotoUtils.createPhoto(d);
			long index = (Long) d.getField("index").numericValue()-start;
			
			ret.put((int)index,new IndexedPhoto(index, p));
		}
		reader.close();
		return ret ;
	}

	/**
	 * Number of documents in a lucene index
	 * @param luceneIndex
	 * @return n docs
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public static int countIndexedItems(String luceneIndex) throws CorruptIndexException, IOException {
		IndexReader reader = null;
		try{
			final Directory directory = new SimpleFSDirectory(new File(luceneIndex));
			reader = DirectoryReader.open(directory);
			return reader.numDocs();
		}finally{			
			reader.close();
		}
	}
}
