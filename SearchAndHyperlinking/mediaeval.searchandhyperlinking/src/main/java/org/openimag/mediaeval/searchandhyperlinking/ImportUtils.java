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
import org.openimaj.io.FileUtils;
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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;



public abstract class ImportUtils {
	
	public static List<SolrInputDocument> readSubtitlesFile(File subsFile)
														   throws SAXException, FileNotFoundException, IOException {
		System.out.println("(Subs) " + subsFile.getName());
		
		XMLReader xr = XMLReaderFactory.createXMLReader();
		
		final List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		final String progName = subsFile.getName().split("\\.")[0];
		
		xr.setContentHandler(new DefaultHandler() {
			SolrInputDocument currentDoc = null;
			int count = 0;
			
			public void startElement(String uri,
									 String localName,
									 String qName,
	                				 Attributes attributes)
	                						 throws SAXException {
				if (localName.equals("p") && currentDoc == null) {
					currentDoc = new SolrInputDocument();
					
					currentDoc.addField("id", progName + "_trans_subtitles_" + count);
					currentDoc.addField("type", "trans");
					currentDoc.addField("program", progName);
					currentDoc.addField("start", HMStoS(attributes.getValue("begin")));
					currentDoc.addField("end", HMStoS(attributes.getValue("end")));
					currentDoc.addField("source", "subtitles");
					
					count++;
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
		System.out.println("(LIMSI) " + limsiFile.getName());
		
		String progName = limsiFile.getName().split("\\.")[0];
		int count = 0;
		
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
								
								solrDoc.addField("id", progName + "_trans_limsi_" + count);
								solrDoc.addField("type", "trans");
								solrDoc.addField("program", progName);
								solrDoc.addField("start", Float.parseFloat(
															segmentElement.getAttribute(
																"stime")));
								solrDoc.addField("end", Float.parseFloat(
															segmentElement.getAttribute(
																"etime")));
								solrDoc.addField("source", "limsi");
								
								count++;
								
								SortedMap<Float, SortedMap<Float, String>> transcriptMatrix = 
										new TreeMap<Float, SortedMap<Float, String>>();
								
								NodeList wordNodeList = segmentElement.getChildNodes();
								for (int k = 0; k < wordNodeList.getLength(); k++) {
									if (wordNodeList.item(k) instanceof Element) {
										Element wordElement = (Element) wordNodeList.item(k);
										
										// Get each Word and group together words occurring
										// at same time (alternatives), maintaining 
										// confidence information.
										if (wordElement.getTagName().equals("Word")) {
											String word = wordElement.getTextContent();
											
											if (word.equals(" {fw} ")) {
												continue;
											}
											
											Float start = Float.parseFloat(
															wordElement.getAttribute("stime"));
											Float conf = Float.parseFloat(
															wordElement.getAttribute("conf"));
											
											SortedMap<Float, String> words =
													transcriptMatrix.get(start);
											
											if (words != null) {
												words.put(conf, word);
											} else {
												words = new TreeMap<Float, String>();
												words.put(conf, word);
												transcriptMatrix.put(start, words);
											}
										}
									}
								}
								
								// Generate most probable transcript from the
								// confidence scores.
								String phrase = "";
								
								for (SortedMap<Float, String> pos : 
										transcriptMatrix.values()) {
									String next = "";
									
									for (String s : pos.values()) {
										next = s;
									}
									
									phrase += next;
								}
								
								solrDoc.addField("phrase", phrase);
								docs.add(solrDoc);
							}
						}
					}
				}
			}
		}
		
		return docs;
	}
	
	public static List<SolrInputDocument> readMetadataFile(File metadataFile) throws IOException {
		System.out.println("(Meta) " + metadataFile.getName());
		
		Gson gson = new Gson();
		
		String json = FileUtils.readall(metadataFile);
		
		LinkedTreeMap deser = gson.fromJson(json, LinkedTreeMap.class);

		List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", deser.get("filename") + "_progmeta");
		doc.addField("type", "progmeta");
		doc.addField("program", deser.get("filename"));
		doc.addField("synopsis", deser.get("description"));
		doc.addField("length", deser.get("duration"));
		doc.addField("title", deser.get("title"));
		
		docs.add(doc);
		
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

	public static float HMStoS(String hmsString) {
		String[] parts = hmsString.split(":");
		
		float secs = Float.parseFloat(parts[2]);
		secs += Float.parseFloat(parts[1]) * 60;
		secs += Float.parseFloat(parts[0]) * 60 * 60;
		
		return secs;
	}
}
