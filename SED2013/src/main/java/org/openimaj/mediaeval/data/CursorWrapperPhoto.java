package org.openimaj.mediaeval.data;

import org.codehaus.staxmate.dom.DOMConverter;
import org.codehaus.staxmate.in.SMInputCursor;
import org.openimaj.mediaeval.data.XMLCursorStream.CursorWrapper;
import org.openimaj.mediaeval.data.util.PhotoUtils;
import org.openimaj.util.function.Function;
import org.w3c.dom.Element;

import com.aetrion.flickr.photos.Photo;

/**
 * Translate a {@link SMInputCursor} to a {@link Element} using {@link DOMConverter}
 * then translate to a {@link Photo} using {@link PhotoUtils#createPhoto(Element)}
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class CursorWrapperPhoto implements Function<CursorWrapper, Photo> {
	DOMConverter dc = new DOMConverter();
	@Override
	public Photo apply(CursorWrapper in) {
		Element e = in.toDoc().getDocumentElement();
		return PhotoUtils.createPhoto(e);
	}

}
