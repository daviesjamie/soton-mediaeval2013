package org.openimag.mediaeval.searchandhyperlinking;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
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
	 */
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
		System.setProperty("solr.solr.home", args[0]);
		CoreContainer.Initializer initializer = new CoreContainer.Initializer();
		CoreContainer coreContainer = initializer.initialize();
		EmbeddedSolrServer server = new EmbeddedSolrServer(coreContainer, "test");
		
		
	}

}
