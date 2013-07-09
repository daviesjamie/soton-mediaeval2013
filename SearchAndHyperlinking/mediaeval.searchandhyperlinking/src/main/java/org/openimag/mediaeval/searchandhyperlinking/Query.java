package org.openimag.mediaeval.searchandhyperlinking;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.common.SolrInputDocument;
import org.openimaj.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class Query {
	private String queryID;
	private String queryText;
	private String visualCues;
	
	public Query(String queryID, String queryText, String visualCues) {
		super();
		this.queryID = queryID;
		this.queryText = queryText;
		this.visualCues = visualCues;
	}
	public String getQueryID() {
		return queryID;
	}
	public void setQueryID(String queryID) {
		this.queryID = queryID;
	}
	public String getQueryText() {
		return queryText;
	}
	public void setQueryText(String queryText) {
		this.queryText = queryText;
	}
	public String getVisualCues() {
		return visualCues;
	}
	public void setVisualCues(String visualCues) {
		this.visualCues = visualCues;
	}
	
	public String toString() {
		return queryText;
	}
	
	public static List<Query> readQueriesFromFile(File queryFile) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = builder.parse(queryFile);
		Element root = document.getDocumentElement();
		
		List<Query> queries = new ArrayList<Query>();
		
		NodeList rootNodeList = root.getChildNodes();
		for (int i = 0; i < rootNodeList.getLength(); i++) {
			if (rootNodeList.item(i) instanceof Element) {
				Element child = (Element) rootNodeList.item(i);

				if (child.getTagName().equals("top")) {
					Query q = new Query(null, null, null);
					
					NodeList childNodeList = child.getChildNodes();
					for (int j = 0; j < childNodeList.getLength(); j++) {
						if (childNodeList.item(j) instanceof Element) {
							Element grandchild = (Element) childNodeList.item(j);
							
							if (grandchild.getTagName().equals("itemId")) {
								q.setQueryID(grandchild.getTextContent());
							} else if (grandchild.getTagName().equals("queryText")) {
								q.setQueryText(grandchild.getTextContent());
							} else if (grandchild.getTagName().equals("visualQueues")) {
								q.setVisualCues(grandchild.getTextContent());
							}
						}
					}
					
					queries.add(q);
				}
			}
		}
		
		return queries;
	}
	
	public List<TestResult> readQueryResultsFromFile(File qrelFile) throws IOException {
		String[] lines = FileUtils.readlines(qrelFile);
		
		List<TestResult> results = new ArrayList<TestResult>();
		
		for (String line : lines) {
			String[] elements = line.split(" ");
			
			Float start = MStoS(elements[2]);
			Float end = MStoS(elements[3]);
			
			results.add(new TestResult(elements[0], elements[1], start, end));
		}
		
		return results;
	}
	
	private float MStoS(String MS) {
		String[] elements = MS.split("\\.");
		
		return (60 * Float.parseFloat(elements[0])) + 
			   Float.parseFloat(elements[1]);
	}
}
