package org.openimaj.mediaeval.placement.data;

import java.util.Iterator;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import org.openimaj.util.stream.AbstractStream;

public class SolrStream extends AbstractStream<SolrDocument> {

    private int resultCount;
    private Iterator<SolrDocument> resultIterator;

    public SolrStream( SolrQuery query, SolrServer server ) throws SolrServerException {
        QueryResponse response = server.query( query );
        SolrDocumentList results = response.getResults();
        resultCount = (int) results.getNumFound();
        resultIterator = results.iterator();
    }

    @Override
    public boolean hasNext() {
        return resultIterator.hasNext();
    }

    @Override
    public SolrDocument next() {
        return resultIterator.next();
    }

    public int getNumResults() {
        return resultCount;
    }

}
