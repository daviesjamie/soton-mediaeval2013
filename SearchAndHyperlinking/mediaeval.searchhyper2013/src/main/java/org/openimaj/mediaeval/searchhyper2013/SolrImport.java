package org.openimaj.mediaeval.searchhyper2013;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;
import org.openimaj.io.FileUtils;
import org.xml.sax.SAXException;

public class SolrImport {
	private static final int CACHE_SIZE = 1000000;
	
	public static enum ImportType {
		Subtitles, LIMSI, Metadata
	}

	/**
	 * Import data from the directory specified on the command line into a 
	 * Solr server running at the given URL.
	 * 
	 * @param args[0]	  Solr server URL. 
	 * @param args[1]	  Path to file dir.
	 * @param args[2]	  Type of files to import.
	 * @param args[3]	  Path to exclusion list.
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		File filesDir = new File(args[1]);
		String type = args[2];
		File excludes = new File(args[3]);
		
		List<String> excludeList = Arrays.asList(FileUtils.readlines(excludes));
		
		List<File> files = removeExcluded(filesDir, excludeList);

		SolrServer server = new HttpSolrServer(args[0]);
		
		ImportType importType= null;
		if (type.equals("subs")) {
			importType = ImportType.Subtitles;
		} else if (type.equals("limsi")) {
			importType = ImportType.LIMSI;
		} else if (type.equals("meta")) {
			importType = ImportType.Metadata;
		}
		
		importFiles(server, files, importType);
		
		server.commit();
	}
	
	public static List<File> removeExcluded(File dir, List<String> excludes) {
		List<File> files = new ArrayList<File>();
		for (File file : dir.listFiles()) {
			if (!excludes.contains(file.getName().split("\\.")[0])) {
				files.add(file);
			}
		}
		
		return files;
	}

	public static void importFiles(SolrServer server,
								   List<File> files,
								   ImportType type)
										   throws Exception {
		List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		
		int count = 1;
		
		for (File sourceFile : files) {
			if (docs.size() >= CACHE_SIZE) {
				server.add(docs);
				docs.clear();
			}
			
			System.out.print(count + "/" + files.size() + " | ");
			List<SolrInputDocument> sourceDocs = null;
			
			switch (type) {
				case Subtitles: sourceDocs = ImportUtils.readSubtitlesFile(sourceFile); break;
				case LIMSI: sourceDocs = ImportUtils.readLIMSIFile(sourceFile); break;
				case Metadata: sourceDocs = ImportUtils.readMetadataFile(sourceFile); break;
				default: throw new Exception("Type not implemented!");
			}
			
			count++;
			
			if (!sourceDocs.isEmpty()) {
				docs.addAll(sourceDocs);
			} else {
				System.out.println("^- Empty!");
			}
		}
		
		server.add(docs);
	}
	
	public static void importFile(SolrServer server,
								  File file,
								  ImportType type) throws Exception {
		List<File> files = new ArrayList<File>();
		files.add(file);
		
		importFiles(server, files, type);
	}
}