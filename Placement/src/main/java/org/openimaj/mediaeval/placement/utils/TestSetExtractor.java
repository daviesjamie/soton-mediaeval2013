package org.openimaj.mediaeval.placement.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


public class TestSetExtractor {
     
    private static final File TEST_ID_FILE = new File( "data/test5" );
    private static final File OUTPUT_FILE = new File( "data/testset.csv" );
    
    private ArrayList<File> inputFiles;
    private String delimiter;
    
    private ArrayList<Long> testIds;

    public TestSetExtractor( ArrayList<File> inputFiles, String delimiter ) throws IOException {
        this.inputFiles = inputFiles;
        this.delimiter = delimiter;
        
        this.testIds = new ArrayList<Long>();
        
        // Read test IDs into memory
        System.out.println( "Reading test IDs from " + TEST_ID_FILE.getName() + " into memory." );
        BufferedReader br = new BufferedReader( new FileReader( TEST_ID_FILE ) );
        String line;
        int count = 0;
        while( ( line = br.readLine() ) != null ) {
            this.testIds.add( Long.parseLong( line ) );
            count++;
        }
        br.close();
        System.out.println( "Read " + count + " test IDs." );
    }
    
    public void run() throws IOException {
        BufferedWriter out = new BufferedWriter( new FileWriter( OUTPUT_FILE ) );
        
        for( File inputFile : inputFiles ) {
            BufferedReader in = new BufferedReader( new FileReader( inputFile ) );
            String line;
            
            System.out.println( "Parsing " + inputFile.getName() );
            
            // Skip header line in each file
            in.readLine();
            
            while( ( line = in.readLine() ) != null ) {
                String[] parts = line.split( delimiter );
                
                if( testIds.contains( Long.parseLong( parts[ 0 ] ) ) ) {
                    out.write( line + "\n" );
                    System.out.println( parts[ 0 ] );
                }
            }
            
            in.close();
        }
        
        out.flush();
        out.close();
        
        System.out.println( "Done! Test data written to " + OUTPUT_FILE.getName() );
    }
    
    public static void main( String args[] ) throws IOException {
        ArrayList<File> inputFiles = new ArrayList<File>();
        
        for( int i = 1; i < 10; i++ )
            inputFiles.add( new File( "data/metadata_" + i + ".csv" ) );

        TestSetExtractor tse = new TestSetExtractor( inputFiles, "," );
        tse.run();
    }
    
}
