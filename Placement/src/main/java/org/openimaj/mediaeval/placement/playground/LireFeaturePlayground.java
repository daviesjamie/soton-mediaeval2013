package org.openimaj.mediaeval.placement.playground;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.openimaj.image.ImageUtilities;
import org.openimaj.mediaeval.placement.data.LireFeatures;
import org.openimaj.mediaeval.placement.search.LireSearcher;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;

/**
 * Demonstrate simple LIRE search
 * 
 * @author Jamie Davies (jagd1g11@ecs.soton.ac.uk)
 * 
 */
public class LireFeaturePlayground {

    public static void main( String[] args ) throws IOException {
        final String lireIndex = "data/lire-feature-index";
        final String luceneIndex = "data/lucene-test-index";

        final int numResults = 10;

        final Directory directory = new SimpleFSDirectory( new File( luceneIndex ) );
        final IndexSearcher luceneSearcher = new IndexSearcher( DirectoryReader.open( directory ) );
        final LireSearcher lireSearcher = new LireSearcher( lireIndex, LireFeatures.CEDD, luceneSearcher, numResults );

        System.out.println( "Ready for query: " );
        String line;
        final BufferedReader br = new BufferedReader( new InputStreamReader( System.in ) );
        while( (line = br.readLine()) != null ) {
            line = line.trim();
            if( line.length() == 0 )
                continue;

            ScoreDoc[] results = null;
            try {
                final long query = Long.parseLong( line );
                results = lireSearcher.search( query, numResults );
            } catch( final NumberFormatException e ) {
                try {
                    final URL u = new URL( line );
                    results = lireSearcher.search( ImageUtilities.readMBF( u ), numResults );
                } catch( final MalformedURLException ee ) {
                    continue;
                }
            }

            for( int i = 0; i < results.length; i++ ) {
                final ScoreDoc r = results[i];
                final Document d = luceneSearcher.doc( r.doc );

                System.out.println( d.get( LuceneIndexBuilder.FIELD_URL ) + "\t" + r.score );
            }
            System.out.println();
            System.out.println( "Ready for query: " );
        }
    }
}
