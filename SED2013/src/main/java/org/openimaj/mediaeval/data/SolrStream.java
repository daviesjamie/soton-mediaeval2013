package org.openimaj.mediaeval.data;

import java.util.Iterator;

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

public class SolrStream extends AbstractStream<SolrDocument>{

	private SolrServer server;
	private SolrQuery query;
	private SolrDocumentList res;
	private Iterator<SolrDocument> resIter;
	private int seen;
	private long nres;

	public SolrStream(SolrQuery q, SolrServer s) {
		this.server = s;
		this.query = q;
		this.seen = 0;

		try {
			QueryResponse ret = doQuery(0);
			this.nres = ret.getResults().getNumFound();
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private QueryResponse doQuery(int start) throws SolrServerException {
		this.query.setStart(start);
		return this.server.query(this.query);
	}

	@Override
	public boolean hasNext() {
		return this.resIter==null || this.resIter.hasNext() || seen < nres;
	}

	@Override
	public SolrDocument next() {
		if(resIter == null || !resIter.hasNext()){
			try {
				QueryResponse ret = this.doQuery(seen);
				this.resIter = ret.getResults().iterator();
				return next();
			} catch (SolrServerException e) {
			}
		}
		SolrDocument toRet = this.resIter.next();
		seen++;
		return toRet;

	}

	public static void main(String[] args) {
		try{
			SolrQuery q = new SolrQuery("*:*");
			q.setSortField("index", ORDER.asc);
			SED2013Index ind = SED2013Index.instance("http://localhost:8983/solr/sed2013_train");
			SolrStream s = new SolrStream(q, ind.getSolrIndex());
			s.map(new SolrDocumentToIndexedPhoto())
			.forEach(new Operation<IndexedPhoto>() {

				@Override
				public void perform(IndexedPhoto object) {
					System.out.println(object.first);
				}
			}, new Not<IndexedPhoto>(new Head<IndexedPhoto>(103)));
		}
		finally{
			SED2013Index.shutdown();
		}
	}

	public int getNumResults() {
		return (int) this.nres;
	}

}
