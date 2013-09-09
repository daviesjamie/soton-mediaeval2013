package org.openimaj.mediaeval.data;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index.IndexedPhoto;
import org.openimaj.stream.functions.Not;
import org.openimaj.util.function.Operation;
import org.openimaj.util.stream.AbstractStream;

public class LuceneStream extends AbstractStream<Document >{

	private Iterator<SolrDocument> resIter;
	private int seen;
	private long nres;
	private Collector col;
	private TopDocs result;
	private int current;
	private DirectoryReader reader;

	public LuceneStream(String indexFile, Query luceneQuery) throws IOException {
		SimpleFSDirectory directory = new SimpleFSDirectory(new File(indexFile));
		this.reader = DirectoryReader.open(directory);
		final IndexSearcher searcher = new IndexSearcher(reader);
		this.result = searcher.search(luceneQuery, reader.numDocs());
	}

	@Override
	public boolean hasNext() {
		return current < this.result.totalHits;
	}

	@Override
	public Document next() {
		Document toRet;
		try {
			toRet = reader.document(this.result.scoreDocs[seen].doc);
			seen++;
			return toRet;
		} catch (CorruptIndexException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public static void main(String[] args) {
		try{
//			LuceneStream s = new LuceneStream(q, ind.getSolrIndex());
//			s.map(new LuceneDocumentToIndexedPhoto())
//			.forEach(new Operation<IndexedPhoto>() {
//
//				@Override
//				public void perform(IndexedPhoto object) {
//					System.out.println(object.first);
//				}
//			}, new Not<IndexedPhoto>(new Head<IndexedPhoto>(103)));
		}
		finally{
			SED2013Index.shutdown();
		}
	}

	public int getNumResults() {
		return (int) this.nres;
	}

}
