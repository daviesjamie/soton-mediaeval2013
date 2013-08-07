package org.openimaj.mediaeval.searchhyper2013.OLD;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;

public abstract class LIUMImportTest {

	public static void main(String[] args) throws IOException {
		List<SolrInputDocument> inputDocs = ImportUtils.readLIUMFile(new File(args[0]));
	
		double[][] data = new double[inputDocs.size()][2];
		
		for (int i = 0; i < inputDocs.size(); i++) {
			data[i][0] = Double.parseDouble(Float.toString((Float) inputDocs.get(i).getFieldValue("start")));
			data[i][1] = Double.parseDouble(Float.toString((Float) inputDocs.get(i).getFieldValue("end")));
			System.out.println(data[i][0] + "-" + data[i][1] + ": " + inputDocs.get(i).getFieldValue("phrase"));
		}
		
		MBFImage vis = DataUtils.visualiseData(data, 31*60f, RGBColour.RED);
		DisplayUtilities.display(vis);
	}

}
