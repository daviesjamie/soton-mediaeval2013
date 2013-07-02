package org.openimag.mediaeval.searchandhyperlinking;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;
import org.xml.sax.SAXException;

/**
 * Class for testing Solr functionality.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class SolrTest {

	/**
	 * 
	 * @param args[0] - Path to Solr home.
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SolrServerException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException, SolrServerException, InterruptedException {
		/*System.setProperty("solr.solr.home", args[0]);
		CoreContainer.Initializer initializer = new CoreContainer.Initializer();
		CoreContainer coreContainer = initializer.initialize();
		EmbeddedSolrServer server = new EmbeddedSolrServer(coreContainer, "");
		*/
		
		SolrServer server = new HttpSolrServer("http://127.0.0.1:8983/solr");
		
		//List<SolrInputDocument> docs = TranscriptUtils.readSubtitlesFile(new File("/home/jpreston/Work/data/mediaeval/mediaeval-searchhyper/collection/subtitles/xml/20080511_001500_bbcthree_two_pints_of_lager_and.xml"));
		List<SolrInputDocument> docs = TranscriptUtils.readLIMSIFile(new File(args[0]));
		
		server.add(docs);
		server.commit();
		
		System.out.println(docs.size());
		
		/*Thread.sleep(5000);
		
		SolrQuery query = new SolrQuery();
		query.setQuery("*:*");
		
		QueryResponse response = server.query(query);
		for (SolrDocument doc : response.getResults()) {
			System.out.println("------");
			
			for (Map.Entry<String, Object> field : doc) {
				System.out.println(field.getKey() + ": " + field.getValue());
			}
			
			System.out.println("------");
		}*/
	}

}
