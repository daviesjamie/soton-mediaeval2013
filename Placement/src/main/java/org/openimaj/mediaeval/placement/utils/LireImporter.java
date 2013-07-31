package org.openimaj.mediaeval.placement.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.semanticmetadata.lire.imageanalysis.LireFeature;
import net.semanticmetadata.lire.indexing.LireCustomCodec;
import net.semanticmetadata.lire.utils.LuceneUtils;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;


/**
 * Imports the task's precalculated image features into a Lucene index using
 * LIRE.
 * 
 * @author Jamie Davies (jagd1g11@ecs.soton.ac.uk)
 */
public class LireImporter {
    /** The LIRE classes that represent the features */
    private static final String[] FIELDS = new String[] {
        "net.semanticmetadata.lire.imageanalysis.AutoColorCorrelogram",     // 0
        "net.semanticmetadata.lire.imageanalysis.BasicFeatures",            // 1
        "net.semanticmetadata.lire.imageanalysis.CEDD",                     // 2
        "net.semanticmetadata.lire.imageanalysis.ColorLayout",              // 3
        "net.semanticmetadata.lire.imageanalysis.EdgeHistogram",            // 4
        "net.semanticmetadata.lire.imageanalysis.FCTH",                     // 5
        "net.semanticmetadata.lire.imageanalysis.FuzzyOpponentHistogram",   // 6
        "net.semanticmetadata.lire.imageanalysis.Gabor",                    // 7
        "net.semanticmetadata.lire.imageanalysis.JointHistogram",           // 8
        "net.semanticmetadata.lire.imageanalysis.JointOpponentHistogram",   // 9
        "net.semanticmetadata.lire.imageanalysis.ScalableColor",            // 10
        "net.semanticmetadata.lire.imageanalysis.SimpleColorHistogram",     // 11
        "net.semanticmetadata.lire.imageanalysis.Tamura"                    // 12
        };
    
    /** The name to use for each feature */
    private static final String[] FIELDNAMES = new String[] {
        "AutoColorCorrelogram",     // 0
        "BasicFeatures",            // 1
        "CEDD",                     // 2
        "ColorLayout",              // 3
        "EdgeHistogram",            // 4
        "FCTH",                     // 5
        "FuzzyOpponentHistogram",   // 6
        "Gabor",                    // 7
        "JointHistogram",           // 8
        "JointOpponentHistogram",   // 9
        "ScalableColor",            // 10
        "SimpleColorHistogram",     // 11
        "Tamura"                    // 12
    };
    
    /**
     * Static prefixes to add to the start of each extracted feature string to
     * fix the format for LIRE.
     */
    private static final String[] PREFIXES = new String[] {
        "",                 // 0
        "",                 // 1
        "cedd ",            // 2
        "",                 // 3
        "edgehistogram;",   // 4
        "fcth ",            // 5
        "",                 // 6
        "gabor ",           // 7
        "jhist ",           // 8
        "",                 // 9
        "scalablecolor;",   // 10
        "RGB ",             // 11
        "tamura "           // 12
    };
    
    /** List of features (by their index) to skip importing */
    private static final List<Integer> SKIP = Arrays.asList( 1, 6, 8, 9 );

    /** The regular expression used to split the different fields in a line */
    private static final String SPLITTER =
            " (acc|bf|cedd|col|edgehistogram|fcth|ophist|gabor|jhist|jophist|scalablecolor|RGB|tamura) ";
    
    /** The path to the Lucene index */
    private String indexPath;

    /** The files to import from */
    private List<File> inputFiles;
    
    /**
     * Basic constructor.
     * Sets the path to the Lucene index, and the files to import.
     * 
     * @param indexPath
     *            The path to the Lucene index (should be a directory).
     * @param inputFiles
     *            A {@link List} of {@link File}s to import.
     */
    public LireImporter( String indexPath, List<File> inputFiles ) {
        this.indexPath = indexPath;
        this.inputFiles = inputFiles;
    }

    /**
     * Starts the import process. Initialises an {@link IndexWriter} and
     * sequentially adds the contents of each file to it.
     */
    public void run() {
        try {
            IndexWriterConfig config = new IndexWriterConfig( LuceneUtils.LUCENE_VERSION, new WhitespaceAnalyzer( LuceneUtils.LUCENE_VERSION ) );
            config.setOpenMode( IndexWriterConfig.OpenMode.CREATE_OR_APPEND );
            config.setCodec( new LireCustomCodec() );
            IndexWriter indexWriter = new IndexWriter( FSDirectory.open( new File( indexPath ) ), config );

            Iterator<File> iterator = inputFiles.iterator();
            while( iterator.hasNext() ) {
                File inputFile = iterator.next();
                System.out.println( "Processing " + inputFile.getPath() + "." );
                readFile( indexWriter, inputFile );
                System.out.println( "Indexing finished." );
            }

            indexWriter.commit();
            indexWriter.close();
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Reads in each image's features contained in a file into a Lucene
     * {@link Document}, and then adds that document to the supplied
     * {@link IndexWriter}.
     * 
     * @param indexWriter
     *            The index to add the image features to.
     * @param inputFile
     *            The file to extract the image features from.
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    private void readFile( IndexWriter indexWriter, File inputFile ) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        BufferedReader br = new BufferedReader( new FileReader( inputFile ), 18000 );
        String line = null;
        int count = 0;
        
        while( ( line = br.readLine() ) != null ) {
            Document d = new Document();
            String[] parts = line.split( SPLITTER );
            
            // PhotoID
            d.add( new StoredField( "photoID", parts[ 0 ] ) );
            
            // Features
            for( int i = 1; i < parts.length; i++ ) {
                // Skip broken features
                if( SKIP.contains( i - 1 ) )
                    continue;
                
                // Fix the formatting so that LIRE will accept the input
                String feature = PREFIXES[ i - 1 ] + parts[ i ];
                if( i-1 == 10 ) {
                    feature = feature.replaceFirst( " ", ";" );
                    feature = feature.replaceFirst( " ", ";" );
                }
                
                LireFeature f = (LireFeature) Class.forName( FIELDS[ i - 1 ] ).newInstance();
                
                try {
                    f.setStringRepresentation( feature );
                } catch( Exception e ) {
                    System.err.println( parts[ 0 ] );
                    e.printStackTrace();
                }
                
                d.add( new StoredField( FIELDNAMES[ i - 1 ], f.getByteArrayRepresentation() ) );
            }

            indexWriter.addDocument( d );
            
            count++;
            
            if( count % 1000 == 0 ) System.out.print( '.' );
            if( count % 10000 == 0 ) System.out.println( " " + count );
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