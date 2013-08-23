package org.openimaj.mediaeval.evaluation.solr.tool;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.lingala.zip4j.core.ZipFile;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openimaj.io.FileUtils;
import org.openimaj.mediaeval.evaluation.datasets.SED2013SimilarityMatrix;
import org.openimaj.mediaeval.evaluation.solr.SED2013Index;
import org.openimaj.mediaeval.evaluation.solr.SED2013SolrIndexCreator;
import org.openimaj.mediaeval.evaluation.solr.SED2013SolrSimilarityMatrix;
import org.openimaj.mediaeval.evaluation.solr.SimilarityMatrixWrapper;
import org.openimaj.vis.general.HeatMap;

import ch.akuhn.matrix.SparseMatrix;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class TestSolrSimilarityTool {
	/**
	 * the output folder
	 */
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private File index;
	private File luceneIndex;
	private Map<String, SparseMatrix> allmats;
	private File data;
	private HashMap<String, File> simmats;
	private File experiments;
	
	/**
	 * @throws Exception
	 */
	@Before
	public void before() throws Exception {
		
		File rootFile = folder.getRoot();
		this.data = new File(rootFile,"data");data.mkdirs();
		this.experiments = new File(rootFile,"experiments/task1");
		File gt = new File(data,"train.csv");
		File train = new File(data,"train.xml");
		File tfidf = new File(data,"tfidf");
		System.out.println("Root folder created: " + data.getAbsolutePath());
		
		File featuresZip = new File(data, "features.zip");
		FileUtils.copyStreamToFileBinary(rec("/training.partial.featurecache.zip"), featuresZip);
		ZipFile zipFile = new ZipFile(featuresZip);
		zipFile.extractAll(data.getAbsolutePath());
		FileUtils.copyStreamToFile(rec("/sed2013_partial_dataset_train_gs.csv"), gt);
		FileUtils.copyStreamToFile(rec("/sed2013_partial_dataset_train.xml"), train);
		
		// Build the SOLR index
		
		File solr = new File(rootFile,"solr");solr.mkdirs();
		index = new File(solr,"index");
		System.setProperty("sed2013.solr.home", index.getAbsolutePath());
		SED2013SolrIndexCreator.main(new String[]{train.getAbsolutePath(),gt.getAbsolutePath()});
		this.luceneIndex = new File(index,"data/index");
		
		// Build the similarity matricies
//		
		FileUtils.copyStreamToFileBinary(rec("/training.sed2013.photo_tfidf"), tfidf);
		SED2013SolrSimilarityMatrix solrSimMat = new SED2013SolrSimilarityMatrix(tfidf.getAbsolutePath(), new File(data, "training.all.featurecache").getAbsolutePath());
		allmats = solrSimMat.createSimilarityMatricies();
		solrSimMat.write(new File(data, "training.all.sparse").getAbsolutePath(),allmats);
//		File sparseZip = new File(data, "sparse.zip");
//		FileUtils.copyStreamToFileBinary(rec("/training.all.sparse.zip"), sparseZip);
//		ZipFile sparseZipFile = new ZipFile(sparseZip);
//		sparseZipFile.extractAll(data.getAbsolutePath());
		File[] sparsefiles = new File(data,"training.all.sparse").listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".mat");
			}
		});
		
		this.simmats = new HashMap<String, File>();
		for (File file : sparsefiles) {
			simmats.put(file.getName(), file);
		}
		
//		System.out.println("Done loading!");
		
		
	}
	
	/**
	 * @throws Exception 
	 * 
	 */
	@Test
	public void test() throws Exception{
		for (Entry<String, File> es : this.simmats.entrySet()) {
			displaySimmat(es.getKey(),es.getValue());
		}
		File file = this.simmats.get("aggregationMean.mat");
		
		SolrSimilarityExperimentTool.main(new String[]{
			"-sm",file.getAbsolutePath(),
			"-si",this.luceneIndex.getAbsolutePath(),
			"-o",this.experiments.getAbsolutePath(),
			"-em","DBSCAN"
		});
		System.out.println("done");
	}

	private void displaySimmat(String name, File file) throws IOException {
		HeatMap hm = new HeatMap(800, 800);
		SimilarityMatrixWrapper similarityMatrix = new SimilarityMatrixWrapper(file.getAbsolutePath(), 0, -1);
		hm.setData(similarityMatrix.matrix().asArray());
		hm.showWindow(name);
	}

	private InputStream rec(String name) {
		return TestSolrSimilarityTool.class.getResourceAsStream(name);
	}
	
	public static void main(String[] args) throws Exception {
		TestSolrSimilarityTool test = new TestSolrSimilarityTool();
		test.before();
		test.test();
	}
}
