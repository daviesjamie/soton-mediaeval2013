package org.openimaj.mediaeval.searchhyper2013.lucene.conversion;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;
import org.openimaj.mediaeval.searchhyper2013.util.Time;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class SubtitlesFileDocumentConverter implements FileDocumentConverter {

	@Override
	public Document convertFile(File subsFile) throws FileDocumentConverterException {
		try {
			return _convertFile(subsFile);
		} catch (Exception e) {
			throw new FileDocumentConverterException(e);
		}
	}
	
	private Document _convertFile(File subsFile) throws Exception {
		System.out.println("(Subs) " + subsFile.getName());
		
		XMLReader xr = XMLReaderFactory.createXMLReader();
		
		final Document doc = new Document();
		final String progName = subsFile.getName().split("\\.")[0];
		
		doc.add(new StringField(Field.Program.toString(),
								progName,
								org.apache.lucene.document.Field.Store.YES));
		doc.add(new StringField(Field.Type.toString(),
								Type.Subtitles.toString(),
								org.apache.lucene.document.Field.Store.YES));
		
		final StringBuilder words = new StringBuilder();
		final StringBuilder times = new StringBuilder();
		
		xr.setContentHandler(new DefaultHandler() {
			float pStart = 0f;
			float pEnd = 0f;
			int count = 0;
			
			public void startElement(String uri,
									 String localName,
									 String qName,
	                				 Attributes attributes)
	                						 throws SAXException {
				if (localName.equals("p")) {
					pStart = Time.HMStoS(attributes.getValue("begin"));
					pEnd = Time.HMStoS(attributes.getValue("end"));
					count = 0;
				}
			}
			
			@Override
			public void endElement(String uri,
								   String localName,
								   String qName)
										   throws SAXException {
				if (localName.equals("p")) {
					for (int i = 0; i < count; i++) {
						float wordTime = pStart + ((pEnd - pStart) / count);
						
						times.append(wordTime + " ");
					}
				}
			}

			public void characters(char[] ch,
								   int start,
								   int length)
										   throws SAXException {
				char[] chars = Arrays.copyOfRange(ch, start, start + length);
				
				words.append(chars);
				count += new String(chars).split(" ").length;
			}
		});
		
		xr.parse(new InputSource(new FileReader(subsFile)));

		doc.add(new TextField(Field.Text.toString(),
							  words.toString(),
							  org.apache.lucene.document.Field.Store.YES));
		doc.add(new StringField(Field.Times.toString(),
				  				times.toString(),
				  				org.apache.lucene.document.Field.Store.YES));
		
		return doc;
	}

}
