package org.openimaj.mediaeval.placement.lire;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import net.semanticmetadata.lire.imageanalysis.LireFeature;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;

public class LireFeatureCalculator {

    final Directory directory = new SimpleFSDirectory( new File( "data/lucene-meta-index" ) );

    private LireFeature feature;
    private IndexSearcher meta;

    public LireFeatureCalculator( LireFeature feature ) throws IOException {
        this.feature = feature;
        final IndexReader reader = DirectoryReader.open(directory);
        meta = new IndexSearcher(reader);
    }

    public byte[] extractByteFeature( long flickrId ) throws Exception {
        Query q = NumericRangeQuery.newLongRange( LuceneIndexBuilder.FIELD_ID, flickrId, flickrId, true, true );
        int did = meta.search( q, 1 ).scoreDocs[0].doc;
        String url = meta.doc( did ).get( LuceneIndexBuilder.FIELD_URL );

        BufferedImage image = ImageIO.read( new URL( url ) );

        feature.extract( image );

        return feature.getByteArrayRepresentation();
    }
    
    public float[] extractFloatFeature( long flickrId ) throws Exception {
        Query q = NumericRangeQuery.newLongRange( LuceneIndexBuilder.FIELD_ID, flickrId, flickrId, true, true );
        int did = meta.search( q, 1 ).scoreDocs[ 0 ].doc;
        String url = meta.doc( did ).get( LuceneIndexBuilder.FIELD_URL );
        
        BufferedImage image = ImageIO.read( new URL( url ) );
        
        feature.extract( image );
        
        String[] parts = feature.getStringRepresentation().split( " " );
        float[] fv = new float[ parts.length ];
        for( int i = 0; i < parts.length; i++ ) {
            fv[ i ] = Float.parseFloat( parts[ i ] );
        }
        
        return fv;
    }
}
