package org.openimaj.util.data.dataset;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.data.dataset.URLListDataset;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;

public class GoogleImagesDatasetBuilder implements
		DatasetBuilder<GroupedDataset<String, ListDataset<MBFImage>, MBFImage>> {

	@Override
	public GroupedDataset<String, ListDataset<MBFImage>, MBFImage> build(
			String[] args, int maxSize) throws BuildException {
		
		final int resultsPerPage = 8;
		final String searchURL = "https://ajax.googleapis.com/ajax/services/search/images?v=1.0&rsz=" + resultsPerPage + "&q=";
		
		HttpClient client = new DefaultHttpClient();
		
		GroupedDataset<String, ListDataset<MBFImage>, MBFImage> dataset = 
			new MapBackedDataset<String, ListDataset<MBFImage>, MBFImage>();
		
		 System.out.println("Attempting to retrieve at least " + maxSize + " images per query.");
		
		for (String query : args) {
			System.out.println("Query: " + query);
			
			URLListDataset<MBFImage> queryDataset = new URLListDataset<MBFImage>(ImageUtilities.MBFIMAGE_READER);
			
			for (int i = 0; i < maxSize; i += resultsPerPage) {				
				final HttpGet request = new HttpGet(searchURL + query + "&start=" + i);
				
				HttpResponse response;
				try {
					response = client.execute(request);
				} catch (IOException e) {
					throw new BuildException(e);
				}
				
				HttpEntity entity = response.getEntity();
				
				if (entity != null) {
					String strJson;
					try {
						strJson = EntityUtils.toString(entity);
					} catch (ParseException | IOException e) {
						throw new BuildException(e);
					}
					
					JSONObject json;
					try {
						json = new JSONObject(strJson);
					} catch (JSONException e) {
						throw new BuildException(e);
					}
					
					JSONArray results;
					try {
						results = json.getJSONObject("responseData").getJSONArray("results");
					} catch (JSONException e) {
						throw new BuildException(e);
					}
					
					for (int j = 0; j < results.length(); j++) {
						JSONObject result;
						try {
							result = (JSONObject) results.get(j);
						} catch (JSONException e) {
							throw new BuildException(e);
						}
						
						URL url;
						try {
							url = new URL(result.getString("url"));
						} catch (MalformedURLException | JSONException e) {
							throw new BuildException(e);
						}
						
						System.out.println((i+j+1) + " : " + url);
						queryDataset.addURL(url);
					}
				}
			}
			
			dataset.put(query, queryDataset);
		}
		
		return dataset;
	}
}
