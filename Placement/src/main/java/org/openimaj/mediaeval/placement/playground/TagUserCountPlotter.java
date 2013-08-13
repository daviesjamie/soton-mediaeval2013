package org.openimaj.mediaeval.placement.playground;

import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.FastScatterPlot;
import org.jfree.ui.ApplicationFrame;

public class TagUserCountPlotter {
    public static void main(String[] args) throws IOException, ParseException {
        final Directory directory = new SimpleFSDirectory(new File("data/lucene-test-index"));

        final Query q = new QueryParser(Version.LUCENE_43, "tags", new StandardAnalyzer(Version.LUCENE_43))
                .parse("+southampton +snow");

        // 3. search
        final int hitsPerPage = 100000;
        final IndexReader reader = DirectoryReader.open(directory);
        final IndexSearcher searcher = new IndexSearcher(reader);
        final TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
        searcher.search(q, collector);
        final ScoreDoc[] hits = collector.topDocs().scoreDocs;

        // 4. display results
        System.out.println( "Found " + collector.getTotalHits() + " hits." );
        final List<float[]> data = new ArrayList<float[]>(7800000);
        
        for( int i = 0; i < hits.length; ++i ) {
            final int docId = hits[ i ].doc;
            final Document d = searcher.doc( docId );
            final String[] llstr = d.get( "location" ).split( " " );
            final float x = Float.parseFloat( llstr[ 0 ] );
            final float y = Float.parseFloat( llstr[ 1 ] );
            data.add( new float[] { x, y } );
        }
        
        final float[][] dataArr = new float[2][data.size()];
        for (int i = 0; i < data.size(); i++) {
            dataArr[0][i] = data.get(i)[0];
            dataArr[1][i] = data.get(i)[1];
        }
        
        final NumberAxis domainAxis = new NumberAxis("Longitude");
        domainAxis.setRange(-180, 180);
        final NumberAxis rangeAxis = new NumberAxis("Latitude");
        rangeAxis.setRange(-90, 90);
        final FastScatterPlot plot = new FastScatterPlot( dataArr, domainAxis, rangeAxis );
        plot.setDomainGridlinesVisible( false );
        plot.setRangeGridlinesVisible( false );
        
        final JFreeChart chart = new JFreeChart("Fast Scatter Plot", plot);
        chart.getRenderingHints().put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(1920, 1200));
        final ApplicationFrame frame = new ApplicationFrame("Title");
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setVisible(true);

        // reader can only be closed when there
        // is no need to access the documents any more.
        reader.close();
    }
}
