package org.openimaj.mediaeval.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.codehaus.staxmate.SMInputFactory;
import org.openimaj.data.dataset.StreamingDataset;
import org.openimaj.mediaeval.data.XMLCursorStream.CursorWrapper;
import org.openimaj.util.function.Operation;
import org.openimaj.util.stream.AbstractStream;
import org.openimaj.util.stream.Stream;

import com.aetrion.flickr.photos.Photo;

/**
 * Present an XML file conaining Flickr Photos as a dataset
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class XMLFlickrPhotoStreamDataset extends AbstractStream<Photo> implements StreamingDataset<Photo> {




	private int total;
	private Stream<Photo> photoStream;

	/**
	 * @param xmlFile
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	public XMLFlickrPhotoStreamDataset(File xmlFile) throws  IOException, XMLStreamException{
		this(new FileInputStream(xmlFile), countPhoto(xmlFile));
	}
	static XMLInputFactory factory = XMLInputFactory.newInstance();
	static SMInputFactory smFact = new SMInputFactory(factory);
	private static int countPhoto(File xml) throws IOException {
		final int[] count = {0};
		try {
			new XMLCursorStream(xml, "photo").forEach(new Operation<CursorWrapper>() {

				@Override
				public void perform(CursorWrapper object) {
					count[0]++;
				}
			});

			return count[0];
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * @param is
	 * @param total total number of photos in this stream
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	public XMLFlickrPhotoStreamDataset(InputStream is, int total) throws IOException, XMLStreamException {
		this.total = total;
		this.photoStream = new XMLCursorStream(is, "photo").map(new CursorWrapperPhoto());
	}

	/**
	 * @param ps a {@link Stream} of {@link Photo} instances
	 * @param total total number of photos in this stream
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	public XMLFlickrPhotoStreamDataset(Stream<Photo> ps, int total) throws IOException, XMLStreamException {
		this.total = total;
		this.photoStream = ps;
	}

		@Override
	public int numInstances() {
		return this.total;
	}

	@Override
	public Photo getRandomInstance() {
		return next();
	}

	@Override
	public boolean hasNext() {
		return this.photoStream.hasNext();
	}

	@Override
	public Photo next() {
		return this.photoStream.next();
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws ParseException
	 * @throws XMLStreamException
	 */
	public static void main(String[] args) throws IOException, ParseException, XMLStreamException {
		String bigFile = "/Volumes/data/mediaeval/mediaeval-SED2013/sed2013_dataset_train.xml";
		File xmlFile = new File(bigFile);
		final SimpleDateFormat df = new SimpleDateFormat("yyyy MM");
		final Date after = df.parse("2007 01");
		final Date before = df.parse("2007 02");
		new XMLCursorStream(xmlFile,"photo")
		.filter(new CursorDateFilter(after, before))
		.filter(new Head<CursorWrapper>(10))
		.map(new CursorWrapperPhoto())
		.forEach(
		new Operation<Photo>() {
			@Override
			public void perform(Photo object) {
				System.out.println(String.format("%s - %s",object.getId(),object.getOwner().getUsername()));
			}
		});
	}
}
