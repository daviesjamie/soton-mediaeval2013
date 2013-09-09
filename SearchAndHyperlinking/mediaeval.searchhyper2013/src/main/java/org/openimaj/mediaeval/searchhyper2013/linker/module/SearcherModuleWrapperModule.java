package org.openimaj.mediaeval.searchhyper2013.linker.module;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Anchor;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineSet;
import org.openimaj.mediaeval.searchhyper2013.linker.LinkerException;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.LuceneUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;
import org.openimaj.mediaeval.searchhyper2013.searcher.module.SearcherModule;

public class SearcherModuleWrapperModule implements LinkerModule {

	Type TRANSCRIPT_TYPE = Type.Subtitles;
	double CONTEXT_BOOST = 0.8;
	double NAME_BOOST = 1.6;
	
	IndexSearcher indexSearcher;
	SearcherModule searcherModule;
	
	public SearcherModuleWrapperModule(SearcherModule searcherModule, IndexSearcher indexSearcher) {
		this.indexSearcher = indexSearcher;
		this.searcherModule = searcherModule;
	}
	
	@Override
	public TimelineSet search(Anchor q, TimelineSet currentSet)
			throws LinkerException {
		// Get transcript at anchor.
		Document transcriptDoc;
		try {
			transcriptDoc = LuceneUtils.getTypeForProgramme(q.fileName,
															TRANSCRIPT_TYPE,
															indexSearcher);
		} catch (IOException e) {
			throw new LinkerException(e);
		}
		String[] wtimes = transcriptDoc.get(Field.Times.toString()).split(" ");
		String[] words = transcriptDoc.get(Field.Text.toString()).split(" ");
		
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < wtimes.length; i++) {
			float time = Float.parseFloat(wtimes[i]);
			
			if (q.startTime <= time && time <= q.endTime) {
				String word = words[i].replaceAll("\\W", "")
									  .toLowerCase()
									  .trim();
				
				if (!word.isEmpty())
					sb.append(word + " ");
			}
		}
		
		if (q.hasContext) {
			sb.append("(");
			
			for (int i = 0; i < wtimes.length; i++) {
				float time = Float.parseFloat(wtimes[i]);
				
				if (q.contextStartTime <= time && time <= q.contextEndTime) {
					String word = words[i].replaceAll("\\W", "")
							  .toLowerCase()
							  .trim();
		
					if (!word.isEmpty())
						sb.append(word + " ");
				}
			}
			
			sb.deleteCharAt(sb.length());
			
			sb.append(")^" + CONTEXT_BOOST + " ");
		}
		
		if (q.anchorName != null) 
			sb.append("(" + q.anchorName + ")^" + NAME_BOOST);
		
		if (sb.toString().isEmpty()) {
			return currentSet;
		}
		
		Query query = new Query(q.anchorID, sb.toString(), null);
		
		try {
			return searcherModule.search(query, currentSet);
		} catch (SearcherException e) {
			throw new LinkerException(e);
		}
	}

}
