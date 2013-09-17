package org.openimaj.mediaeval.searchhyper2013.datastructures;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openimaj.mediaeval.searchhyper2013.util.Time;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AnchorList extends ArrayList<Anchor> {

	private static final long serialVersionUID = 4107983294029808701L;
	
	public static AnchorList readFromFile(File anchorsFile, boolean withContext) throws ParserConfigurationException, SAXException, IOException {
		AnchorList anchorList = new AnchorList();
		
		DocumentBuilder builder = 
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
		
		org.w3c.dom.Document document = builder.parse(anchorsFile);
		
		Element root = document.getDocumentElement();
		
		// Iterate over top-level nodes.
		NodeList nodeList = root.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i) instanceof Element) {
				Element element = (Element) nodeList.item(i);
				
				String anchorID = null;
				String anchorName = null;
				Float startTime = null;
				Float endTime = null;
				String fileName = null;
				Float contextStartTime = null;
				Float contextEndTime = null;
				
				// We're going into an anchor.
				if (element.getTagName().equals("anchor")) {
					NodeList anchorNodeList = element.getChildNodes();
					for (int j = 0; j < anchorNodeList.getLength(); j++) {
						if (anchorNodeList.item(j) instanceof Element) {
							Element anchorElement = (Element) anchorNodeList.item(j);
							
							String tagName = anchorElement.getTagName();
							String tagText = anchorElement.getTextContent();
							
							if (tagName.equals("anchorId")) {
								anchorID = tagText;
							} else if (tagName.equals("anchorName")) {
								anchorName = tagText;
							} else if (tagName.equals("startTime")) {
								startTime = Time.MStoS(tagText);
							} else if (tagName.equals("endTime")) {
								endTime = Time.MStoS(tagText);
							} else if (tagName.equals("item")) {
								NodeList itemNodeList = anchorElement.getChildNodes();
								for (int k = 0; k < itemNodeList.getLength(); k++) {
									if (itemNodeList.item(k) instanceof Element) {
										Element itemElement = (Element) itemNodeList.item(k);
										
										tagName = itemElement.getTagName();
										tagText = itemElement.getTextContent();
										
										if (tagName.equals("fileName")) {
											fileName = tagText.replaceFirst("v", "");
										} else if (tagName.equals("startTime")) {
											contextStartTime = Time.MStoS(tagText);
										} else if (tagName.equals("endTime")) {
											contextEndTime = Time.MStoS(tagText);
										}
									}
								}
							}
						}
					}
				}
				
				Anchor anchor = new Anchor();
				
				if (contextStartTime != null && withContext) {
					anchor.contextStartTime = contextStartTime;
					anchor.contextEndTime = contextEndTime;
					anchor.hasContext = true;
				}
				
				anchor.anchorID = anchorID;
				anchor.anchorName = anchorName;
				anchor.fileName = fileName;
				anchor.startTime = startTime;
				anchor.endTime = endTime;
				
				anchorList.add(anchor);
			}
		}
	
		return anchorList;		
	}
}
