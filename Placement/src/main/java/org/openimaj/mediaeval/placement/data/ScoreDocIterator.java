package org.openimaj.mediaeval.placement.data;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;


public class ScoreDocIterator implements Iterator<Document>{

    private ScoreDoc[] docs;
    private IndexSearcher searcher;
    private int index;
    
    public ScoreDocIterator( ScoreDoc[] scoredocs, IndexSearcher searcher ) {
        this.docs = scoredocs;
        this.searcher = searcher;
        this.index = 0;
    }

    @Override
    public boolean hasNext() {
        return index < docs.length - 1;
    }

    @Override
    public Document next() {
        if( index < docs.length - 1 ) {
            try {
                return searcher.doc( docs[ index++ ].doc );
            } catch( IOException e ) {
                System.err.println( "Document could not be found." );
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException( "ScoreDocIterator.remove() is not supported!" );
    }

}
