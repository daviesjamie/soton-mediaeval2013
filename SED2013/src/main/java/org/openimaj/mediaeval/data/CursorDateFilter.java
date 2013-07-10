package org.openimaj.mediaeval.data;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openimaj.mediaeval.data.XMLCursorStream.CursorWrapper;
import org.openimaj.mediaeval.data.util.PhotoUtils;
import org.openimaj.util.function.Predicate;

/**
 * Date Predicate
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public final class CursorDateFilter implements Predicate<CursorWrapper> {
	private final Date after;
	private final Date before;

	public CursorDateFilter(Date after, Date before) {
		this.after = after;
		this.before = before;
	}

	@Override
	public boolean test(CursorWrapper object) {
		try {
			String d = object.cursor.getAttrValue("dateUploaded");
			if(d!=null && d.length()!=0){
				Date date = (
					(SimpleDateFormat)PhotoUtils.DATE_FORMATS.get()
				).parse(d);
				if(date.after(after) && date.before(before))
					return true;
			}

		} catch (Exception e) {
		}
		return false;
	}
}