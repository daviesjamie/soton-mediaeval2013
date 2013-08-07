package org.openimaj.mediaeval.searchhyper2013;

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class LIMSIFileDocumentConverter implements FileDocumentConverter {

	@Override
	public Document convertFile(File limsiFile)
			throws FileDocumentConverterException {
		System.out.println("(LIMSI) " + limsiFile.getName());
		
		String progName = limsiFile.getName().split("\\.")[0];
		
		Document doc = new Document();
	
		doc.add(new StringField(Field.Program.toString(),
								progName,
								org.apache.lucene.document.Field.Store.YES));
		doc.add(new StringField(Field.Type.toString(),
								Type.LIMSI.toString(),
								org.apache.lucene.document.Field.Store.YES));						
	
		StringBuilder wordsBuilder = new StringBuilder();
		StringBuilder timesBuilder = new StringBuilder();
		
		DocumentBuilder builder;
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new FileDocumentConverterException(e);
		}
		
		org.w3c.dom.Document document;
		try {
			document = builder.parse(limsiFile);
		} catch (SAXException e) {
			throw new FileDocumentConverterException(e);
		} catch (IOException e) {
			throw new FileDocumentConverterException(e);
		}
		
		Element root = document.getDocumentElement();
		
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
											String word = wordElement.getTextContent().trim();
											
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
								for (Float time : transcriptMatrix.keySet()) {
									timesBuilder.append(time + " ");
									
									SortedMap<Float, String> words = 
											transcriptMatrix.get(time);
									
									wordsBuilder.append(words.get(words.lastKey()) + " ");
								}
							}
						}
					}
				}
			}
		}
	
		doc.add(new TextField(Field.Text.toString(),
				wordsBuilder.toString(),
				org.apache.lucene.document.Field.Store.YES));
		doc.add(new StringField(Field.Times.toString(),
				timesBuilder.toString(),
				org.apache.lucene.document.Field.Store.YES));
		
		return doc;
	}

}
