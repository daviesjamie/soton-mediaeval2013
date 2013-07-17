package org.openimaj.mediaeval.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.codehaus.staxmate.in.SMInputCursor.Tracking;
import org.openimaj.mediaeval.data.XMLCursorStream.CursorWrapper;
import org.openimaj.util.stream.AbstractStream;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Given an {@link SMInputCursor} or an {@link InputStream} and element name (for which a {@link SMInputCursor}
 * is constructed, produce a stream which provides access to the cursor for each element detected.
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class XMLCursorStream extends AbstractStream<CursorWrapper> {

	static XMLInputFactory factory = XMLInputFactory.newInstance();
	static SMInputFactory smFact = new SMInputFactory(factory);
	private SMInputCursor photoCursor;

	/**
	 * A wrapper around a cursor that allows the creation of
	 * a Document from the
	 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
	 *
	 */
	public static class CursorWrapper{
		SMInputCursor cursor;
		static DocumentBuilder db ;
		static{
			try {
				db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			} catch (Exception e) {
			}
		}
		/**
		 * @param photoCursor
		 */
		public CursorWrapper(SMInputCursor photoCursor) {
			this.cursor = photoCursor;
		}
		/**
		 * @return a document for the current position of the cursor
		 */
		public Document toDoc(){
			Document doc = db.newDocument();
			try {
				doc.appendChild(createElement(doc,cursor));
			} catch (Exception e){
				e.printStackTrace();
			}

			return doc;
		}
		private Element createElement(Document doc, SMInputCursor cursor) throws XMLStreamException {
			String localName = cursor.getLocalName();
			Element elm = doc.createElement(localName);
			for (int i = 0; i < cursor.getAttrCount(); i++) {
				elm.setAttribute(cursor.getAttrLocalName(i), cursor.getAttrValue(i));
			}
			SMInputCursor childCursor = cursor.childCursor();
			SMEvent evt;
			while((evt = childCursor.getNext())!=null){
				if(evt.isElementEvent()){
					elm.appendChild(createElement(doc, childCursor));
				}
				else if(evt.isTextualEvent()){
					CDATASection node = doc.createCDATASection(childCursor.getText());
					elm.appendChild(node);
				}
			}
			return elm;
		}
	}
	/**
	 * @param s construct {@link SMHierarchicCursor} around this stream
	 * @param e use {@link SMHierarchicCursor#childElementCursor(javax.xml.namespace.QName)} with this name
	 * @throws XMLStreamException
	 */
	public XMLCursorStream(InputStream s, String e) throws XMLStreamException {
		SMHierarchicCursor rootCursor;
//		try {
//			rootCursor = smFact.rootElementCursor(new InputStreamReader(s, "UTF-8"));
			rootCursor = smFact.rootElementCursor(s);
//		} catch (UnsupportedEncodingException e1) {
//			throw new XMLStreamException(e1);
//		}
		rootCursor.advance();
		this.photoCursor = rootCursor.childElementCursor("photo");
		photoCursor.setElementTracking(Tracking.NONE);
	}
	/**
	 * @param s construct {@link SMHierarchicCursor} around this stream
	 * @param e use {@link SMHierarchicCursor#childElementCursor(javax.xml.namespace.QName)} with this name
	 * @throws XMLStreamException
	 * @throws FileNotFoundException
	 */
	public XMLCursorStream(File s, String e) throws XMLStreamException, FileNotFoundException {
		this(new FileInputStream(s),e);
	}
	/**
	 * @param s stream across this {@link SMInputCursor}
	 * @throws XMLStreamException
	 */
	public XMLCursorStream(SMInputCursor s) throws XMLStreamException {
		this.photoCursor = s;

	}
	@Override
	public boolean hasNext() {
		try {
			photoCursor.getNext();
			SMEvent event = photoCursor.getCurrEvent();
			return event!=null;
		} catch (XMLStreamException e) {
			return false;
		}
	}

	@Override
	public CursorWrapper next() {
		return new CursorWrapper(photoCursor);
	}

}
