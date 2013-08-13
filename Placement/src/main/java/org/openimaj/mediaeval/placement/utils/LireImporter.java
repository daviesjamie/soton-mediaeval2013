package org.openimaj.mediaeval.placement.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.LireFeature;
import net.semanticmetadata.lire.utils.LuceneUtils;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.openimaj.mediaeval.placement.data.LireFeatures;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;


public class LireImporter {

    private static final String[] PREFIXES = new String[] {
        "",
        "",
        "cedd ",
        "",
        "edgehistogram;",
        "fcth ",
        "ophist ",
        "gabor ",
        "jhist ",
        "jophist ",
        "scalablecolor;",
        "RGB ",
        "tamura "
    };

    private static final List<Integer> SKIP = Arrays.asList();
    
    private String indexPath;
    private List<File> inputFiles;
    private List<String> skippedPhotos;

    public LireImporter( String indexPath, List<File> inputFiles ) {
        this.indexPath = indexPath;
        this.inputFiles = inputFiles;
    }

    public void run() {
        try {
            IndexWriterConfig config = new IndexWriterConfig( LuceneUtils.LUCENE_VERSION, new WhitespaceAnalyzer( LuceneUtils.LUCENE_VERSION ) );
            config.setOpenMode( IndexWriterConfig.OpenMode.CREATE_OR_APPEND );
            IndexWriter indexWriter = new IndexWriter( FSDirectory.open( new File( indexPath ) ), config );

            skippedPhotos = new ArrayList<String>();
            Iterator<File> iterator = inputFiles.iterator();

            while( iterator.hasNext() ) {
                File inputFile = iterator.next();
                System.out.println( "Processing " + inputFile.getPath() + "." );
                readFile( indexWriter, inputFile );
                System.out.println( "Indexing finished." );
            }

            indexWriter.close();
        } catch( Exception e ) {
            e.printStackTrace();
            return;
        }

        System.out.println( "Done." );
        System.out.println( skippedPhotos.size() + " photos were skipped." );
        for( String p : skippedPhotos )
            System.out.println( p );
    }

    private void readFile( IndexWriter indexWriter, File inputFile ) throws Exception { 
        BufferedReader br = new BufferedReader( new FileReader( inputFile ), 18000 );
        String line = null;
        String feature = null;
        int count = 0;

        outerLoop:
        while( ( line = br.readLine() ) != null ) {
            Document d = new Document();
            String[] parts = line.split( LireFeatures.CSVREGEX );

            d.add( new StoredField( LuceneIndexBuilder.FIELD_ID, parts[ 0 ] ) );

            for( int i = 1; i < parts.length; i++ ) {
                try {
                    // Skip broken features
                    if( SKIP.contains( i - 1 ) )
                        continue;

                    // Format feature string so that LIRE will accept it
                    feature = PREFIXES[ i - 1 ] + parts[ i ];
                    if( i - 1 == 10 ) {
                        feature = feature.replaceFirst( " ", ";" );
                        feature = feature.replaceFirst( " ", ";" );
                    }

                    LireFeature f = (LireFeature) LireFeatures.values()[ i - 1 ].fclass.newInstance();

                    f.setStringRepresentation( feature );

                    // If basic features, use string representation
                    if( i == 2 )
                        d.add( new StoredField( LireFeatures.values()[ i - 1 ].name, f.getStringRepresentation() ) );
                    else
                        d.add( new StoredField( LireFeatures.values()[ i - 1 ].name, f.getByteArrayRepresentation() ) );
                } catch( Throwable e ) {
                    e.printStackTrace();
                    // System.exit( 0 );
                    skippedPhotos.add( parts[ 0 ] );
                    continue outerLoop;
                }
            }
            
            indexWriter.addDocument( d );
            count++;

            if( count % 1000 == 0 )
                System.out.print( '.' );

            if( count % 10000 == 0 ) {
                indexWriter.commit();
                System.out.println( " " + count );
            }
        }

        br.close();
    }

    public static void main( String[] args ) {
        ArrayList<File> inputFiles = new ArrayList<File>();
        for( int i = 1; i < 10; i++ )
            inputFiles.add( new File( "data/imagefeatures_" + i ) );

        LireImporter li = new LireImporter( "data/lire-feature-index", inputFiles );
        li.run();
    }
}
