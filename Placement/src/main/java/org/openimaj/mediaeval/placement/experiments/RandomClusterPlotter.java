package org.openimaj.mediaeval.placement.experiments;

import org.openimaj.mediaeval.placement.vis.ClusterPlot;

public class RandomClusterPlotter {
    
    private ClusterPlot cp;
    private int numClusters;
    private double maxSize;
    private double minSize;
    
    public RandomClusterPlotter( int numClusters, double maxSize, double minSize ) {
        cp = new ClusterPlot( 1920, 1200, false );
        this.numClusters = numClusters;
        this.maxSize = maxSize;
        this.minSize = minSize;
    }
    
    public void plot() {
        for( int i = 0; i < numClusters; i++ ) {
            cp.addCluster( ( Math.random() * 180 ) - 90, ( Math.random() * 360 ) - 180,
                    minSize + ( Math.random() * ( maxSize - minSize ) ) );
        }
        
        cp.addPoint( ( Math.random() * 180 ) - 90, ( Math.random() * 360 ) - 180 );
        
        cp.display();
    }

    public static void main( String[] args ) {
        new RandomClusterPlotter( 5, 1.5, 10 ).plot();
    }
}
