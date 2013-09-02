package org.openimaj.mediaeval.searchhyper2013.searcher.module;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.search.ScoreDoc;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Timeline;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineSet;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;

public class ChannelFilterModule implements SearcherModule {

	Pattern bbcPattern =
			Pattern.compile("bbc\\s*(one|1|two|2|three|3|four|4)?",
							Pattern.CASE_INSENSITIVE);
	
	@Override
	public TimelineSet search(Query q, TimelineSet currentSet)
			throws SearcherException {
		
		Matcher bbcMatcher = bbcPattern.matcher(q.queryText);
		
		String channelString = null;
		
		while (bbcMatcher.find()) {
			String channel = bbcMatcher.group(1);
			
			if (channel != null) {
				if (channel.equals("1")) {
					channel = "one";
				} else if (channel.equals("2")) {
					channel = "two";
				} else if (channel.equals("3")) {
					channel = "three";
				} else if (channel.equals("4")){
					channel = "four";
				}
				
				channelString = "bbc" + channel;
			}
		}
		
		TimelineSet timelines = currentSet;
		
		if (channelString != null) {
			timelines = new TimelineSet();
			
			for (Timeline timeline : currentSet) {
				if (timeline.getID().contains(channelString)) {
					timelines.add(timeline);
				}
			}
		}
		
		return timelines;
	}

}
