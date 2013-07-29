package org.openimaj.mediaeval.placement.experiments;
import java.awt.Color;
import java.awt.RenderingHints;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.FastScatterPlot;
import org.jfree.ui.ApplicationFrame;
import org.openimaj.math.geometry.point.Coordinate;
import org.openimaj.mediaeval.placement.data.SolrStream;
import org.openimaj.util.function.Operation;

public class SolrQueryPlotter {
    
    public static void main(String[] args) throws IOException, SolrServerException {
        final List<float[]> data = new ArrayList<float[]>(7563041);

        SolrServer server = new HttpSolrServer( "http://localhost:8983/solr/placementimages" );
        SolrQuery query = new SolrQuery();
        query.setQuery( "photoTags:ironbridge" );
        query.setStart( 0 ).setRows( 8000000 );
        
        SolrStream s = new SolrStream( query, server );
        s.forEach( new Operation<SolrDocument>() {
            
            @Override
            public void perform( SolrDocument doc ) {
                if( doc.get( "location" ) != null ) {
                    final String[] parts = ((String) doc.get( "location" )).split( "," );
    
                    final float longitude = Float.parseFloat( parts[1] );
                    final float latitude = Float.parseFloat( parts[0] );
    
                    data.add( new float[] { longitude, latitude } );
                }
            }
        } );
        
        QueryResponse rsp = server.query( query );
        SolrDocumentList docs = rsp.getResults();
        
        System.out.println( docs.getNumFound() );
        
        for( SolrDocument doc : docs ) {
            if( doc.get( "location" ) != null ) {
                final String[] parts = ((String) doc.get( "location" )).split( "," );
                
                final float longitude = Float.parseFloat( parts[1] );
                final float latitude = Float.parseFloat( parts[0] );
                
                data.add( new float[] { longitude, latitude } );
            }
        }
        
        System.out.println("Done reading");

        final float[][] dataArr = new float[2][data.size()];
        for (int i = 0; i < data.size(); i++) {
            dataArr[0][i] = data.get(i)[0];
            dataArr[1][i] = data.get(i)[1];
        }

        final NumberAxis domainAxis = new NumberAxis("X");
        domainAxis.setRange(-180, 180);
        final NumberAxis rangeAxis = new NumberAxis("Y");
        rangeAxis.setRange(-90, 90);
        final FastScatterPlot plot = new FastScatterPlot(dataArr, domainAxis, rangeAxis);
        plot.setPaint( new Color( 1f, 0f, 0f, 1f ) );
        plot.setDomainGridlinesVisible( false );
        plot.setRangeGridlinesVisible( false );
        plot.setRangePannable( true );
        plot.setDomainPannable( true );
        
        

        final JFreeChart chart = new JFreeChart("Fast Scatter Plot", plot);
        chart.getRenderingHints().put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(1920, 1200));
        final ApplicationFrame frame = new ApplicationFrame("Title");
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setVisible(true);
    }
}
