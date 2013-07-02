package org.openimag.mediaeval.searchandhyperlinking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;


public abstract class TranscriptUtils {

	public static List<SolrInputDocument> readSubtitlesFile(File subsFile)
														   throws SAXException, FileNotFoundException, IOException {
		XMLReader xr = XMLReaderFactory.createXMLReader();
		
		final List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		final String progName = subsFile.getName().split("\\.")[0];
		
		xr.setContentHandler(new DefaultHandler() {
			SolrInputDocument currentDoc = null;
			
			public void startElement(String uri,
									 String localName,
									 String qName,
	                				 Attributes attributes)
	                						 throws SAXException {
				if (qName.equals("p") && currentDoc == null) {
					currentDoc = new SolrInputDocument();
					
					//doc.addField("id", val);
					currentDoc.addField("program", progName);
					//doc.addField("phrase", );
					currentDoc.addField("start", HMStoS(attributes.getValue("begin")));
					currentDoc.addField("end", HMStoS(attributes.getValue("end")));
					currentDoc.addField("source", "subtitles");
				}
			}
			
			public void characters(char[] ch,
								   int start,
								   int length)
										   throws SAXException {
				if (currentDoc != null) {
					currentDoc.addField("phrase",
										String.valueOf(ch, start, length));
					docs.add(currentDoc);
					
					currentDoc = null;
				}
			}
		});
		
		xr.parse(new InputSource(new FileReader(subsFile)));
		
		return docs;
	}
	
	public static List<SolrInputDocument> readLIMSIFile(File limsiFile)
			throws FileNotFoundException, IOException, ParserConfigurationException, SAXException {
		String progName = limsiFile.getName().split("\\.")[0];
		
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		
		Document document = builder.parse(limsiFile);
		
		Element root = document.getDocumentElement();
		
		List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		
		// Iterate over top-level nodes.
		NodeList nodeList = root.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i) instanceof Element) {
				Element element = (Element) nodeList.item(i);
				
				// We're going into a SegmentList.
				if (element.getTagName().equals("SegmentList")) {
					NodeList segmentNodeList = element.getChildNodes();
					for (int j = 0; j < segmentNodeList.getLength(); j++) {
						if (segmentNodeList.item(j) instanceof Element) {
							Element segmentElement = (Element) segmentNodeList.item(j);
							
							// We've got a Segment.
							if (segmentElement.getTagName().equals("SpeechSegment")) {
								
								// Set up doc properties.
								SolrInputDocument solrDoc = new SolrInputDocument();
								
								solrDoc.addField("program", progName);
								solrDoc.addField("start", Float.parseFloat(
															segmentElement.getAttribute(
																"stime")));
								solrDoc.addField("end", Float.parseFloat(
															segmentElement.getAttribute(
																"etime")));
								solrDoc.addField("source", "limsi");
								
								SortedMap<Float, Set<String>> transcriptMatrix = 
										new TreeMap<Float, Set<String>>();
								
								NodeList wordNodeList = segmentElement.getChildNodes();
								for (int k = 0; k < wordNodeList.getLength(); k++) {
									if (wordNodeList.item(k) instanceof Element) {
										Element wordElement = (Element) wordNodeList.item(k);
										
										// Get each Word and group together words occurring
										// at same time (alternatives).
										if (wordElement.getTagName().equals("Word")) {
											String word = wordElement.getTextContent();
											
											Float start = Float.parseFloat(
															wordElement.getAttribute("stime"));
											
											Set<String> words = transcriptMatrix.get(start);
											
											if (words != null) {
												words.add(word);
											} else {
												words = new HashSet<String>();
												words.add(word);
												transcriptMatrix.put(start, words);
											}
										}
									}
								}
								
								// Generate list of all possible transcripts.
								List<List<String>> transcripts =
										generatePermutations(
												new ArrayList<Set<String>>(
														transcriptMatrix.values()));
								
								// Build each doc.
								for (List<String> transcript : transcripts) {
									String phrase = "";
									
									for (String word : transcript) {
										phrase += word;
									}
									
									/*SolrInputDocument newDoc = solrDoc.deepCopy();
									newDoc.addField("phrase", phrase);
									
									docs.add(newDoc);*/
									
									solrDoc.addField("phrase", phrase);
								}
								
								docs.add(solrDoc);
							}
						}
					}
				}
			}
		}
		
		return docs;
	}
	
	public static <T> List<List<T>> generatePermutations(List<Set<T>> sets) {
		return genPerms(sets, new ArrayList<T>());
	}
	
	private static <T> List<List<T>> genPerms(List<Set<T>> remainingSets, List<T> acc) {
		if (remainingSets.isEmpty()) {
			List<List<T>> result = new ArrayList<List<T>>();
			result.add(acc);
			
			return result;
		}
		
		Set<T> curSet = remainingSets.get(0);
		
		List<List<T>> perms = new ArrayList<List<T>>();
		for (T item : curSet) {
			List<T> newAcc = new ArrayList<T>(acc);
			newAcc.add(item);
			
			List<Set<T>> newRemainingSets = new ArrayList<Set<T>>(remainingSets);
			newRemainingSets.remove(0);
			
			perms.addAll(genPerms(newRemainingSets, newAcc));
		}
		
		return perms;
	}
	
	public static <T> List<List<T>> procGenPerms(List<Set<T>> sets) {
		if (sets.isEmpty()) return null;
		
		List<List<T>> results = new ArrayList<List<T>>();
		
		Set<T> initSet = sets.remove(0);
		for (T item : initSet) {
			List<T> initList = new ArrayList<T>();
			initList.add(item);
			results.add(initList);
		}
		
		for (Set<T> set : sets) {
			List<List<T>> acc = new ArrayList<List<T>>();
			
			for (T item : set) {
				List<List<T>> iter = new ArrayList<List<T>>(results);
				
				// AAARGH COPYING!!!!
				for (int i = 0; i < iter.size(); i++) {
					List<T> copy = new ArrayList<T>(iter.get(i));
					copy.add(item);
					iter.set(i, copy);
				}
				
				acc.addAll(iter);
			}
			
			results = acc;
		}
		
		return results;
	}
	
	public static void main(String[] args) {
		Set<String> lvl1 = new HashSet<String>();
		lvl1.add("A");
		lvl1.add("B");
		lvl1.add("C");
		
		Set<String> lvl2 = new HashSet<String>();
		lvl2.add("1");
		lvl2.add("2");
		lvl2.add("3");
		
		Set<String> lvl3 = new HashSet<String>();
		lvl3.add("dog");
		lvl3.add("cat");
		lvl3.add("pig");
		
		List<Set<String>> sets = new ArrayList<Set<String>>();
		sets.add(lvl1);
		sets.add(lvl2);
		sets.add(lvl3);
		
		List<List<String>> iter = generatePermutations(sets);
		//List<List<String>> proc = procGenPerms(sets);
		
		for (List<String> l : iter) {
			for (String s : l) {
				System.out.println(s);
			}
			
			System.out.println("---\n");
		}
	}

	public static float HMStoS(String hmsString) {
		String[] parts = hmsString.split(":");
		
		float secs = Float.parseFloat(parts[2]);
		secs += Float.parseFloat(parts[1]) * 60;
		secs += Float.parseFloat(parts[0]) * 60 * 60;
		
		return secs;
	}
}
