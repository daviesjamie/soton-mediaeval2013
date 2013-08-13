package org.openimaj.mediaeval.placement.data;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.openimaj.util.stream.AbstractStream;


public class LuceneStream extends AbstractStream<Document> {

    private int resultCount;
    private Iterator<Document> resultIterator;
    
    public LuceneStream( Query query, IndexSearcher index, int maxHits ) throws IOException {
        TopDocs results = index.search( query, maxHits );   
        resultCount = results.totalHits;
        resultIterator = new ScoreDocIterator( results.scoreDocs, index );
    }

    @Override
    public boolean hasNext() {
        return resultIterator.hasNext();
    }

    @Override
    public Document next() {
        return resultIterator.next();
    }
    
    public int getNumResults() {
        return resultCount;
    }

}
