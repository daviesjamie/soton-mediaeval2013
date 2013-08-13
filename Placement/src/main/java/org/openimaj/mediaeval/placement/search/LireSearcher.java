package org.openimaj.mediaeval.placement.search;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.ImageSearcher;
import net.semanticmetadata.lire.impl.GenericFastImageSearcher;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.mediaeval.placement.data.LireFeatures;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;
import uk.ac.soton.ecs.jsh2.mediaeval13.placing.search.VisualSearcher;

public class LireSearcher implements VisualSearcher {

    private IndexReader index;
    private ImageSearcher searcher;
    private IndexSearcher meta;

    public LireSearcher( String lireIndex, LireFeatures feature, IndexSearcher meta, int maxHits ) throws IOException {
        this.index = DirectoryReader.open( FSDirectory.open( new File( lireIndex )  ) );
        this.meta = meta;

        this.searcher = new GenericFastImageSearcher( maxHits, feature.fclass, feature.name );
    }

    @Override
    public ScoreDoc[] search( MBFImage query, int numResults ) throws IOException {
        BufferedImage bquery = ImageUtilities.createBufferedImageForDisplay( query );
        DisplayUtilities.display( bquery, "Query image" );

        ImageSearchHits hits = searcher.search( bquery, index );

        return linkResults( hits );
    }

    @Override
    public ScoreDoc[] search( long flickrId, int numResults ) throws IOException {
        System.out.println( "Query: " + flickrId );
        final Query q = NumericRangeQuery.newLongRange( LuceneIndexBuilder.FIELD_ID, flickrId, flickrId, true, true );
        int docId = meta.search( q, 1 ).scoreDocs[ 0 ].doc;
        
        ImageSearchHits hits = searcher.search( meta.doc( docId ), index );
        
        return linkResults( hits );
    }

    private ScoreDoc[] linkResults( ImageSearchHits hits ) throws IOException {
        final ScoreDoc[] docs = new ScoreDoc[ hits.length() ];

        for( int i = 0; i < docs.length; i++ ) {
            final Document hit = hits.doc( i );
            long photoId = Long.parseLong( hit.get( LuceneIndexBuilder.FIELD_ID ) );
            final Query q = NumericRangeQuery.newLongRange( LuceneIndexBuilder.FIELD_ID, photoId, photoId, true, true );
            docs[ i ] = meta.search( q, 1 ).scoreDocs[ 0 ];
            docs[ i ].score = hits.score( i );
        }

        return docs;
    }

}
