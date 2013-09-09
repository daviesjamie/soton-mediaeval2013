package org.openimaj.mediaeval.searchhyper2013.searcher.module;

import gov.sandia.cognition.learning.algorithm.clustering.AgglomerativeClusterer;
import gov.sandia.cognition.learning.algorithm.clustering.cluster.DefaultCluster;
import gov.sandia.cognition.learning.algorithm.clustering.hierarchy.ClusterHierarchyNode;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.QueryTermScorer;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.openimaj.knn.DoubleNearestNeighboursExact;
import org.openimaj.knn.DoubleNearestNeighboursExact.Factory;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Query;
import org.openimaj.mediaeval.searchhyper2013.datastructures.Timeline;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineFactory;
import org.openimaj.mediaeval.searchhyper2013.datastructures.TimelineSet;
import org.openimaj.mediaeval.searchhyper2013.lucene.Field;
import org.openimaj.mediaeval.searchhyper2013.lucene.LuceneUtils;
import org.openimaj.mediaeval.searchhyper2013.lucene.TimeStringFormatter;
import org.openimaj.mediaeval.searchhyper2013.lucene.Type;
import org.openimaj.mediaeval.searchhyper2013.searcher.SearcherException;
import org.openimaj.mediaeval.searchhyper2013.util.Time;
import org.openimaj.ml.clustering.dbscan.DBSCAN;
import org.openimaj.ml.clustering.dbscan.DoubleDBSCANClusters;
import org.openimaj.ml.clustering.dbscan.DoubleNNDBSCAN;
import org.openimaj.util.pair.FloatObjectPair;
import org.openimaj.util.pair.ObjectDoublePair;

import de.jungblut.clustering.AgglomerativeClustering;
import de.jungblut.clustering.AgglomerativeClustering.ClusterNode;
import de.jungblut.distance.EuclidianDistance;
import de.jungblut.math.DoubleVector;
import de.jungblut.math.dense.DenseDoubleVector;

public class TranscriptModule implements SearcherModule {

	Type TRANSCRIPT_TYPE;
	double TRANSCRIPT_WEIGHT = 0.15;
	double TRANSCRIPT_POWER = 2;
	double TRANSCRIPT_WIDTH = 60;
	double FRAGMENT_DENSITY_FACTOR = 1;
	
	Version LUCENE_VERSION = Version.LUCENE_43;
	
	StandardQueryParser queryParser;
	IndexSearcher indexSearcher;
	Analyzer analyzer;
	Directory spellDir;
	TimelineFactory timelineFactory;
	
	public TranscriptModule(IndexSearcher indexSearcher,
							Type transcriptType,
							Analyzer analyzer,
							Directory spellDir,
							TimelineFactory timelineFactory) {
		this.indexSearcher = indexSearcher;
		this.analyzer = analyzer;
		this.spellDir = spellDir;
		this.timelineFactory = timelineFactory;
		
		TRANSCRIPT_TYPE = transcriptType;
		
		queryParser = new StandardQueryParser(
						new EnglishAnalyzer(LUCENE_VERSION));
	}
	
	@Override
	public TimelineSet search(Query q, TimelineSet currentSet)
			throws SearcherException {
		try {
			return _search2(q, currentSet);
		} catch (Exception e) {
			throw new SearcherException(e);
		}
	}
	
	public TimelineSet _search2(Query q, TimelineSet currentSet) 
														throws Exception {
		String query = LuceneUtils.fixQuery(
				QueryParser.escape(
						ChannelFilterModule.removeChannel(
								q.queryText)),
				spellDir);

		System.out.println(query);
		
		org.apache.lucene.search.Query luceneQuery = 
			queryParser.parse(query,
							  Field.Text.toString());
		
		System.out.println(luceneQuery);
		
		Filter transcriptFilter = new QueryWrapperFilter(
								new TermQuery(
									new Term(Field.Type.toString(),
											 TRANSCRIPT_TYPE.toString())));
		
		ScoreDoc[] hits = LuceneUtils.normaliseTopDocs(
						indexSearcher.search(luceneQuery,
											 transcriptFilter,
											 10));
		
		TimelineSet timelines = new TimelineSet(currentSet);
		
		for (ScoreDoc doc : hits) {
			Document luceneDocument = indexSearcher.doc(doc.doc);
			
			//System.out.println("Transcript hit: " + luceneDocument.get(Field.Program.toString()));
			
			String id = luceneDocument.get(Field.Program.toString());
			
			Timeline programmeTimeline =
					timelines.getTimelineWithID(id);
			
			if (programmeTimeline == null) {
				programmeTimeline =	timelineFactory.makeTimeline(id);
			}
			
			// Remove title terms.
			Document synopsisDocument =
					LuceneUtils.getSynopsisForProgramme(id, indexSearcher);
			String[] titleWords =
					synopsisDocument.get(Field.Title.toString()).split(" ");
			
			String[] queryParts = query.split(" ");
			
			StringBuilder sb = new StringBuilder();
			
			Pattern pattern = Pattern.compile("(\\w+)");
			
			query:
			for (String queryPart : queryParts) {
				Matcher matcher = pattern.matcher(queryPart);
				
				while (matcher.find()) {
					for (String titleWord : titleWords) {
						if (titleWord.toLowerCase().replaceAll("\\W", "")
										.equals(
							matcher.group(1).toLowerCase())) {
							
							continue query;
						}
					}
				}
				
				sb.append(queryPart + " ");
			}
			
			query = sb.toString().trim();
			
			luceneQuery = queryParser.parse(query,
											Field.Text.toString());
			
			String transcript = luceneDocument.get(Field.Text.toString());
			
			Highlighter timeHighlighter = 
					new Highlighter(
						new TimeStringFormatter(
							query,
							transcript,
							luceneDocument.get(Field.Times.toString()),
							indexSearcher.getIndexReader(),
							doc.doc),
						new DefaultEncoder(),
						new QueryTermScorer(luceneQuery));
			TokenStream tokenStream =
					analyzer.tokenStream(Field.Text.toString(),
										 new StringReader(transcript));
			
			String timeString = 
					timeHighlighter.getBestFragments(tokenStream,
													 transcript,
													 1000000,
													 " ");
			
			Map<Float, ObjectDoublePair<String>> transHits = 
					TimeStringFormatter.extractHits(timeString);
			
			/*double[][] times = new double[transHits.size()][1];

			for (int i = 0; i < times.length; i++) {
				times[i][0] = (double) transHits.get(i).getFirst();
			}
			
			DoubleNNDBSCAN dbscan = 
				new DoubleNNDBSCAN(60 * 5,
								   1,
								   new DoubleNearestNeighboursExact.Factory());
			int[][] clusters = dbscan.cluster(times).clusters();*/
			
			AgglomerativeClusterer<Float, DefaultCluster<Float>> clusterer = 
					new AgglomerativeClusterer<Float, DefaultCluster<Float>>();
			ClusterHierarchyNode<Float, DefaultCluster<Float>> root = 
					clusterer.clusterHierarchically(transHits.keySet());
			
			/*List<DoubleVector> data = new ArrayList<DoubleVector>();
			
			for (Float time : transHits.keySet()) {
				data.add(new DenseDoubleVector(1, time));
			}
			
			ClusterNode rootCluster =
					AgglomerativeClustering.cluster(data,
													new EuclidianDistance(),
													false)
										   .get(0)
										   .get(0);*/
			
			Queue<ClusterHierarchyNode<Float, DefaultCluster<Float>>> queue = 
					new ConcurrentLinkedQueue<ClusterHierarchyNode<Float, DefaultCluster<Float>>>();
			queue.add(root);
			
			final double MAXIMUM_CLUSTER_DISTANCE = 60;
			
			while (!queue.isEmpty()) {
				ClusterHierarchyNode<Float, DefaultCluster<Float>> node = 
						queue.remove();
				
				if (getDivergence(node) < MAXIMUM_CLUSTER_DISTANCE) {
					// Extract times from this node.
					List<Float> times = new ArrayList<Float>();
					
					Queue<ClusterHierarchyNode<Float, DefaultCluster<Float>>> 
						nodeQueue = new ConcurrentLinkedQueue<ClusterHierarchyNode<Float, DefaultCluster<Float>>>();
					nodeQueue.add(node);
					
					while (!nodeQueue.isEmpty()) {
						ClusterHierarchyNode<Float, DefaultCluster<Float>> 
							current = nodeQueue.remove();
						
						if (getDivergence(current) > 0) {
							nodeQueue.add(current.getChildren().get(0));
							nodeQueue.add(current.getChildren().get(1));
						} else {
							times.add((float) getMean(current));
						}
					}
					
					// Process.
					float startTime = Float.MAX_VALUE;
					float endTime = 0;
					double score = 0;
					
					Set<String> uniqueHits = new HashSet<String>();

					for (Float time : times) {
						String word = transHits.get(time).getFirst();
						double wordScore = transHits.get(time).getSecond();
						
						startTime = Math.min(startTime,  time);
						endTime = Math.max(endTime, time);
						score += wordScore;
						
						uniqueHits.add(word.toLowerCase());
					}
					
					score *= ((double) uniqueHits.size()) /
							query.split(" ").length;
				
					TranscriptFunction function = 
						new TranscriptFunction(TRANSCRIPT_WEIGHT * doc.score *
									 			 Math.pow(score,
									 					  TRANSCRIPT_POWER),
									 		   startTime + ((endTime - startTime) / 2),
									 		   ((endTime - startTime) + TRANSCRIPT_WIDTH)
										 		   		/ 3d);
					programmeTimeline.addFunction(function);
					
					function.addJustification(
							"Transcript matched at " + Time.StoMS(startTime) + " to " + 
							Time.StoMS(endTime) + " with score " + score + " and " + 
							"match: " + TimeStringFormatter.getFragment(timeString,
																		startTime,
																		endTime));
				} else {
					queue.add(node.getChildren().get(0));
					queue.add(node.getChildren().get(1));
				}
			}
			
			/*
			for (int i = 0; i < clusters.length; i++) {
				float startTime = Float.MAX_VALUE;
				float endTime = 0;
				double score = 0;
				
				Set<String> uniqueHits = new HashSet<String>();
				
				for (int j = 0; j < clusters[i].length; j++) {
					float time = (float) times[clusters[i][j]][0];
					String word = transHits.get(i).getSecond().getFirst();
					double wordScore = transHits.get(i).getSecond().getSecond();
					
					startTime = Math.min(startTime,  time);
					endTime = Math.max(endTime, time);
					score += wordScore;
					
					uniqueHits.add(word.toLowerCase());
				}
				
				score *= ((double) uniqueHits.size()) /
							query.split(" ").length;
				
				TranscriptFunction function = 
					new TranscriptFunction(TRANSCRIPT_WEIGHT * doc.score *
								 			 Math.pow(score,
								 					  TRANSCRIPT_POWER),
								 		   startTime + ((endTime - startTime) / 2),
								 		   ((endTime - startTime) + TRANSCRIPT_WIDTH)
									 		   		/ 3d);
				programmeTimeline.addFunction(function);
				
				function.addJustification(
						"Transcript matched at " + Time.StoMS(startTime) + " to " + 
						Time.StoMS(endTime) + " with score " + score + " and " + 
						"match: " + TimeStringFormatter.getFragment(timeString,
																	startTime,
																	endTime));
			}*/
			
			if (programmeTimeline.numFunctions() > 0) {
				timelines.add(programmeTimeline);
			}
		}
	
		return timelines;
	}
	
	public TimelineSet _search(Query q, TimelineSet currentSet)
														throws Exception {
		String query = LuceneUtils.fixQuery(
							QueryParser.escape(
									ChannelFilterModule.removeChannel(
											q.queryText)),
							spellDir);
		
		System.out.println(query);
		
		org.apache.lucene.search.Query luceneQuery = 
				queryParser.parse(query,
								  Field.Text.toString());
		
		System.out.println(luceneQuery);
		
		Filter transcriptFilter = new QueryWrapperFilter(
									new TermQuery(
										new Term(Field.Type.toString(),
												 TRANSCRIPT_TYPE.toString())));
		
		ScoreDoc[] hits = LuceneUtils.normaliseTopDocs(
							indexSearcher.search(luceneQuery,
												 transcriptFilter,
												 10));
		
		TimelineSet timelines = new TimelineSet(currentSet);
		
		for (ScoreDoc doc : hits) {
			Document luceneDocument = indexSearcher.doc(doc.doc);
			
			//System.out.println("Transcript hit: " + luceneDocument.get(Field.Program.toString()));
			
			String id = luceneDocument.get(Field.Program.toString());
			
			Timeline programmeTimeline =
					timelines.getTimelineWithID(id);
			
			if (programmeTimeline == null) {
				programmeTimeline =	timelineFactory.makeTimeline(id);
			}
			
			// Remove title terms.
			Document synopsisDocument =
					LuceneUtils.getSynopsisForProgramme(id, indexSearcher);
			String[] titleWords =
					synopsisDocument.get(Field.Title.toString()).split(" ");
			
			String[] queryParts = query.split(" ");
			
			StringBuilder sb = new StringBuilder();
			
			Pattern pattern = Pattern.compile("(\\w+)");
			
			query:
			for (String queryPart : queryParts) {
				Matcher matcher = pattern.matcher(queryPart);
				
				while (matcher.find()) {
					for (String titleWord : titleWords) {
						if (titleWord.toLowerCase().replaceAll("\\W", "")
										.equals(
							matcher.group(1).toLowerCase())) {
							
							continue query;
						}
					}
				}
				
				sb.append(queryPart + " ");
			}
			
			query = sb.toString().trim();
			
			luceneQuery = queryParser.parse(query,
											Field.Text.toString());
			
			String transcript = luceneDocument.get(Field.Text.toString());
			
			Highlighter timeHighlighter = 
					new Highlighter(
						new TimeStringFormatter(
							query,
							transcript,
							luceneDocument.get(Field.Times.toString()),
							indexSearcher.getIndexReader(),
							doc.doc),
						new DefaultEncoder(),
						new QueryTermScorer(luceneQuery));
			TokenStream tokenStream =
					analyzer.tokenStream(Field.Text.toString(),
										 new StringReader(transcript));
			
			/*String timeString = 
					timeHighlighter.getBestFragments(tokenStream,
													 transcript,
													 1000000, " ");*/
			TextFragment[] frags =
					timeHighlighter.getBestTextFragments(tokenStream,
														 transcript,
														 true, 
														 100);
			/*String[] frags = timeHighlighter.getBestFragments(tokenStream,
											 				  transcript,
											 				  100);*/
			
			for (TextFragment frag : frags) {
				Map<Float, Double> times = 
						TimeStringFormatter.timesFromString(frag.toString());
				
				float start = Float.MAX_VALUE;
				float end = 0;
				double score = 0;
				
				for (Float time : times.keySet()) {
					if (time > programmeTimeline.getEndTime() + 30) {
						continue;
					}
					
					start = Math.min(start, time);
					end = Math.max(end, time);
					//score = Math.max(score, times.get(time));
					score += times.get(time);
				}
				
				if (start == Float.MAX_VALUE) {
					continue;
				}
				
				score *= TimeStringFormatter.calculateScale(frag.toString(),
															query);
				
				TranscriptFunction function = 
						new TranscriptFunction(TRANSCRIPT_WEIGHT * doc.score *
									 			 Math.pow(score,
									 					  TRANSCRIPT_POWER),
									 		   start + ((end - start) / 2),
									 		   ((end - start) + TRANSCRIPT_WIDTH)
									 		   		/ 3d);
				programmeTimeline.addFunction(function);
				
				function.addJustification(
						"Transcript matched at " + Time.StoMS(start) + " to " + 
						Time.StoMS(end) + " with score " + score + " and " + 
						"match: " + frag);
			}
			
			//System.out.println(timeString + "\n");
			
			tokenStream.close();
			/*
			Map<Float, Double> times =
					TimeStringFormatter.timesFromString(timeString);
			
			for (Float time : times.keySet()) {
				
				// Don't add if it's past the end of the programme.
				if (time > programmeTimeline.getEndTime() + 30) {
					continue;
				}
				
				TranscriptFunction function = 
						new TranscriptFunction(TRANSCRIPT_WEIGHT * doc.score *
									 			 Math.pow(times.get(time),
									 					  TRANSCRIPT_POWER),
									 		   time,
									 		   TRANSCRIPT_WIDTH / 3d);
				programmeTimeline.addFunction(function);
				
				function.addJustification(
						"Transcript matched at " + Time.StoMS(time) + " with score " + 
						times.get(time) + " and " + "match: " +
						TimeStringFormatter.getWordAndContextAtTime(timeString,
																	time));
			}
			*/
			if (programmeTimeline.numFunctions() > 0) {
				timelines.add(programmeTimeline);
			}
		}
		
		return timelines;
	}
	
	public class TranscriptFunction extends Gaussian
									implements JustifiedTimedFunction {
		List<String> justifications;
		
		double mean;
		
		public TranscriptFunction(double norm, double mean, double sigma) {
			super(norm, mean, sigma);
			
			this.mean = mean;
			
			justifications = new ArrayList<String>();
		}
		
		public boolean addJustification(String justification) {
			return justifications.add(justification);
		}
		
		public List<String> getJustifications() {
			return justifications;
		}
		
		public float getTime() {
			return (float) mean;
		}
		
		@Override
		public String toString() {
			return "Transcript function @ " + Time.StoMS((float) mean);
		}
	}
	
	private static float getDivergence(ClusterHierarchyNode<Float, DefaultCluster<Float>> node) throws Exception {
			
		List<ClusterHierarchyNode<Float, DefaultCluster<Float>>> 
			children = node.getChildren();
			
		if (children == null || children.size() != 2) {
			throw new Exception("OSHIT");
		}
		
		return Math.abs(getMean(children.get(0)) - getMean(children.get(1)));
	}
	
	private static float getMean(ClusterHierarchyNode<Float, DefaultCluster<Float>> node) throws Exception {
		Collection<Float> left = node.getMembers();
		
		float avgLeft = 0;
		
		for (Float f : left) {
			avgLeft += f;
		}
		
		avgLeft /= left.size();
		
		return avgLeft;
	}
}
