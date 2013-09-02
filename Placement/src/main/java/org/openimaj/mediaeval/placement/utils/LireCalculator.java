package org.openimaj.mediaeval.placement.utils;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import net.semanticmetadata.lire.imageanalysis.AutoColorCorrelogram;
import net.semanticmetadata.lire.imageanalysis.BasicFeatures;
import net.semanticmetadata.lire.imageanalysis.CEDD;
import net.semanticmetadata.lire.imageanalysis.ColorLayout;
import net.semanticmetadata.lire.imageanalysis.EdgeHistogram;
import net.semanticmetadata.lire.imageanalysis.FCTH;
import net.semanticmetadata.lire.imageanalysis.FuzzyOpponentHistogram;
import net.semanticmetadata.lire.imageanalysis.Gabor;
import net.semanticmetadata.lire.imageanalysis.ScalableColor;
import net.semanticmetadata.lire.imageanalysis.SimpleColorHistogram;
import net.semanticmetadata.lire.imageanalysis.Tamura;
import net.semanticmetadata.lire.imageanalysis.joint.JointHistogram;
import net.semanticmetadata.lire.imageanalysis.joint.RankAndOpponent;
import net.semanticmetadata.lire.utils.LuceneUtils;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.openimaj.mediaeval.placement.data.LireFeatures;

import uk.ac.soton.ecs.jsh2.mediaeval13.placing.indexing.LuceneIndexBuilder;

public class LireCalculator {

    private AutoColorCorrelogram acc;
    private BasicFeatures bf;
    private CEDD cedd;
    private ColorLayout col;
    private EdgeHistogram edge;
    private FCTH fcth;
    private FuzzyOpponentHistogram fop;
    private Gabor gab;
    private JointHistogram jh;
    private RankAndOpponent jop;
    private ScalableColor sc;
    private SimpleColorHistogram sch;
    private Tamura tam;

    private IndexSearcher meta;
    private IndexWriter indexWriter;

    public LireCalculator( String indexPath, IndexSearcher metadata ) throws IOException {
        acc = new AutoColorCorrelogram();
        bf = new BasicFeatures();
        cedd = new CEDD();
        col = new ColorLayout();
        edge = new EdgeHistogram();
        fcth = new FCTH();
        fop = new FuzzyOpponentHistogram();
        gab = new Gabor();
        jh = new JointHistogram();
        jop = new RankAndOpponent();
        sc = new ScalableColor();
        sch = new SimpleColorHistogram();
        tam = new Tamura();

        IndexWriterConfig config = new IndexWriterConfig( LuceneUtils.LUCENE_VERSION, new WhitespaceAnalyzer( LuceneUtils.LUCENE_VERSION ) );
        config.setOpenMode( IndexWriterConfig.OpenMode.CREATE_OR_APPEND );
        indexWriter = new IndexWriter( FSDirectory.open( new File( indexPath ) ), config );
        
        this.meta = metadata;
    }

    public void extractFeatures( long flickrId ) throws MalformedURLException, IOException {
        Query q = NumericRangeQuery.newLongRange( LuceneIndexBuilder.FIELD_ID, flickrId, flickrId, true, true );
        int did = meta.search( q, 1 ).scoreDocs[ 0 ].doc;
        String url = meta.doc( did ).get( LuceneIndexBuilder.FIELD_URL );

        BufferedImage image = ImageIO.read( new URL( url ) );

        Document d = new Document();
        d.add( new StoredField( LuceneIndexBuilder.FIELD_ID, flickrId ) );
        
        acc.extract( image );
        d.add( new StoredField( LireFeatures.ACC.name, acc.getByteArrayRepresentation() ) );

        bf.extract( image );
        d.add( new StoredField( LireFeatures.BF.name, bf.getStringRepresentation() ) );

        cedd.extract( image );
        d.add( new StoredField( LireFeatures.CEDD.name, cedd.getByteArrayRepresentation() ) );

        col.extract( image );
        d.add( new StoredField( LireFeatures.COL.name, col.getByteArrayRepresentation() ) );

        edge.extract( image );
        d.add( new StoredField( LireFeatures.EDGEHISTOGRAM.name, edge.getByteArrayRepresentation() ) );

        fcth.extract( image );
        d.add( new StoredField( LireFeatures.FCTH.name, fcth.getByteArrayRepresentation() ) );

        fop.extract( image );
        d.add( new StoredField( LireFeatures.OPHIST.name, fcth.getByteArrayRepresentation() ) );

        gab.extract( image );
        d.add( new StoredField( LireFeatures.GABOR.name, gab.getByteArrayRepresentation() ) );

        jh.extract( image );
        d.add( new StoredField( LireFeatures.JHIST.name, jh.getByteArrayRepresentation() ) );

        jop.extract( image );
        d.add( new StoredField( LireFeatures.JOPHIST.name, jop.getByteArrayRepresentation() ) );

        sc.extract( image );
        d.add( new StoredField( LireFeatures.SCALABLECOLOR.name, sc.getByteArrayRepresentation() ) );

        sch.extract( image );
        d.add( new StoredField( LireFeatures.RGB.name, sc.getByteArrayRepresentation() ) );

        tam.extract( image );
        d.add( new StoredField( LireFeatures.TAMURA.name, tam.getByteArrayRepresentation() ) );

        indexWriter.addDocument( d );
        indexWriter.commit();
    }

    public static void main( String args[] ) throws IOException {
        File input = new File( "data/featureskipped" );
        BufferedReader br = new BufferedReader( new FileReader( input ) );
        
        String line = null;
        int count = 0;
        int fails = 0;
        
        final IndexReader ir = DirectoryReader.open( new SimpleFSDirectory( new File( "data/lucene-meta-index" ) ) );
        final IndexSearcher metadata = new IndexSearcher( ir );
        LireCalculator lireExtractor = new LireCalculator( "data/lire-feature-index", metadata );
        
        while( ( line = br.readLine() ) != null ) {
            try {
                System.out.println( line );
                lireExtractor.extractFeatures( Long.parseLong( line ) );
                count++;
            } catch( Exception e ) {
                System.err.println( line + " failed" );
                fails++;
            }
        }
        
        br.close();
        
        System.out.println( "\nDone!" );
        System.out.println( count + " photos processed." );
        if( fails > 0 )
            System.err.println( fails + " photos failed." );
    }

}
