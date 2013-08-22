package org.openimaj.mediaeval.searchhyper2013.OLD;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import gov.sandia.cognition.math.matrix.mtj.DenseVector;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.client.solrj.response.TermsResponse.Term;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;

/**
 * Includes various utility methods for searching a Solr server.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public abstract class SearchUtils {

	public static SolrDocumentList queryServer(SolrServer server,
											   String query, 
											   String filterQuery,
											   int maxResults) throws SolrServerException {
		SolrQuery solrQuery = new SolrQuery(query);
		solrQuery.setFilterQueries(filterQuery);
		solrQuery.setRows(maxResults);
		solrQuery.addField("score");
		solrQuery.addField("*");

		QueryResponse response = server.query(solrQuery);
		
		return response.getResults();
	}
	
	public static MapVector getTermVector(SolrServer server, String id) throws SolrServerException {
		SolrQuery solrQuery = new SolrQuery("id:" + id);
		solrQuery.setRequestHandler("/tvrh");
		solrQuery.setParam("tv.tf_idf", true);
		
		QueryResponse response = server.query(solrQuery);
		
		if (response.getResults().size() != 1) return null;
	
		MapVector tfidfVector = new MapVector();
		Map<Object, Double> tfidfMap = tfidfVector.getMap();
		
		NamedList<Object> termVectors =
				(NamedList<Object>) response.getResponse().get("termVectors");
		for (Entry<String, Object> idEntry : termVectors) {
			if (idEntry.getKey().equals(id)) {
				for (Entry<String, Object> phraseEntry : (NamedList<Object>) idEntry.getValue()) {
					if (phraseEntry.getKey().equals("phrase")) {
						for (Entry<String, Object> termEntry : (NamedList<Object>) phraseEntry.getValue()) {
							String key = termEntry.getKey();
							Double val = (Double) ((NamedList<Object>) termEntry.getValue()).get("tf-idf");
							
							tfidfMap.put(key, val);
						}
					}
				}
			}
		}
		
		return tfidfVector;
	}
}
