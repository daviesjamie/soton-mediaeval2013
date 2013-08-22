package org.openimaj.mediaeval.placement.search;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.ImageSearcher;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.mediaeval.placement.data.LireFeatures;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.VisualSearcher;

public class LireSearcher implements VisualSearcher {

    private IndexReader ir;
    private IndexSearcher metaSearch;
    private ImageSearcher visSearch;
    private IndexSearcher featSearch;

    public LireSearcher( String lireIndex, LireFeatures feature, IndexSearcher metaSearch, int maxHits ) throws IOException {
        this.ir =  DirectoryReader.open( FSDirectory.open( new File( lireIndex ) ) );
        this.featSearch = new IndexSearcher( ir );
        this.visSearch = new GenericFastImageSearcher( maxHits, feature.fclass, feature.name );
        this.metaSearch = metaSearch;
    }

    @Override
    public ScoreDoc[] search( MBFImage query, int numResults ) throws IOException {
        BufferedImage bquery = ImageUtilities.createBufferedImage( query );
        ImageSearchHits hits = visSearch.search( bquery, ir );
        
        return linkResults( hits );
    }

    @Override
    public ScoreDoc[] search( long flickrId, int numResults ) throws IOException {
        ImageSearchHits hits = null;
        try {
            final Query q = NumericRangeQuery.newLongRange( LuceneIndexBuilder.FIELD_ID, flickrId, flickrId, true, true );
//            final Query q = new QueryParser( Version.LUCENE_43, LuceneIndexBuilder.FIELD_ID, new StandardAnalyzer( Version.LUCENE_43 ) ).parse( String.valueOf( flickrId ) );
            final TopDocs topdocs = featSearch.search( q, 1 );
            final int docId = topdocs.scoreDocs[ 0 ].doc;
            final Document dquery = featSearch.doc( docId );
            
            hits = visSearch.search( dquery, ir );
        } catch( Exception e ) {
            e.printStackTrace();
        }
        
        return linkResults( hits );
    }
    
    private ScoreDoc[] linkResults( ImageSearchHits hits ) throws IOException {
        final ScoreDoc[] docs = new ScoreDoc[ hits.length() ];
        
        for( int i = 0; i < hits.length(); i++ ) {
            final Document hit = hits.doc( i );
            long photoId = Long.parseLong( hit.get( LuceneIndexBuilder.FIELD_ID ) );
            final Query q = NumericRangeQuery.newLongRange( LuceneIndexBuilder.FIELD_ID, photoId, photoId, true, true );
            docs[ i ] = metaSearch.search( q, 1 ).scoreDocs[ 0 ];
            docs[ i ].score = hits.score( i );
        }
        
        return docs;
    }

}
