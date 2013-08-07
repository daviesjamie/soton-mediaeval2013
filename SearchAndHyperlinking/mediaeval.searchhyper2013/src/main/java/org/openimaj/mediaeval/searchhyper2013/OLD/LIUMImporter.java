package org.openimaj.mediaeval.searchhyper2013.OLD;

import gov.sandia.cognition.learning.algorithm.clustering.AgglomerativeClusterer;
import gov.sandia.cognition.learning.algorithm.clustering.cluster.DefaultCluster;
import gov.sandia.cognition.learning.algorithm.clustering.cluster.DefaultClusterCreator;
import gov.sandia.cognition.learning.algorithm.clustering.divergence.ClusterToClusterDivergenceFunction;
import gov.sandia.cognition.learning.algorithm.clustering.hierarchy.ClusterHierarchyNode;
import gov.sandia.cognition.util.CloneableSerializable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.openimaj.io.FileUtils;

import com.google.common.collect.ForwardingSortedSet;

public class LIUMImporter {
	public static class Word implements Comparable<Word> {
		Float start;
		Float end;
		String word;
		Float confidence;
		
		@Override
		public int compareTo(Word arg0) {
			if (arg0.start < start) {
				return 1;
			} else if (arg0.start > start) {
				return -1;
			} else {
				return 0;
			}
		}
	}
	
	public static class Phrase extends TreeSet<Word> 
							   implements Overlappable<Phrase, Word>,
							   			  Comparable<Phrase> {

		public Phrase() {
			super();
		}
		
		public Phrase(Collection<? extends Phrase> phrases) {
			super();
			
			for (Phrase phrase : phrases) {
				addAll(phrase);
			}
		}
		
		public float startTime() {
			return first().start;
		}
		
		public float endTime() {
			return last().end;
		}
		
		public String phrase() {
			String phrase = "";
			
			for (Word word : this) {
				phrase += word.word + " ";
			}
			
			return phrase;
		}
		
		public float confidence() {
			float confidence = 0f;
			
			for (Word word : this) {
				confidence += word.confidence;
			}
			
			confidence /= size();
			
			return confidence;
		}

		@Override
		public boolean overlappedBy(Phrase obj) {
			if (startTime() < obj.startTime()) {
				return endTime() > obj.startTime();
			} else {
				return obj.endTime() > startTime();
			}
		}

		@Override
		public Collection<Phrase> splitOverlap(Phrase obj) {
			Phrase difference = (Phrase) clone();
			difference.removeAll(obj);
			
			Phrase union = (Phrase) clone();
			union.addAll(obj);
			
			List<Phrase> splitPhrases = new ArrayList<Phrase>();
			
			Phrase current;
			Phrase other;
			
			if (difference.contains(union.first())) {
				current = difference;
				other = obj;
			} else {
				current = obj;
				other = difference;
			}
			
			Phrase acc = new Phrase();
			
			for (Word word : union) {
				if (current.contains(word)) {
					acc.add(word);
				} else {
					splitPhrases.add(acc);
					
					acc = new Phrase();
					acc.add(word);

					Phrase temp = current;
					current = other;
					other = temp;
				}
			}
			
			// Add the last accumulation if it exists.
			if (!acc.isEmpty()) {
				splitPhrases.add(acc);
			}
			
			/*System.out.println("Union: " + union.size());
			System.out.println("Difference: " + difference.size());
			System.out.println("Split: " + splitPhrases.size());
			System.out.println("---");
			*/
			
			
			/*System.out.println(this);
			System.out.println(obj);
			System.out.println(splitPhrases);
			System.out.println("----------");*/
			
			return splitPhrases;
		}

		@Override
		public Collection<Phrase> clipCollection(
				Collection<Phrase> collection) {
			Set<Phrase> clipped = new TreeSet<Phrase>();
			
			for (Phrase phrase : collection) {
				if (overlappedBy(phrase)) {
					clipped.add(phrase);
				}
			}
			
			return clipped;
		}

		@Override
		public int compareTo(Phrase o) {
			float sizeDiff = (o.endTime() - o.startTime()) - 
							 (endTime() - startTime());
			if (sizeDiff > 0) {
				return 1;
			} else if (sizeDiff < 0) {
				return -1;
			} else {
				return 0;
			}
		}
		
		@Override
		public String toString() {
			return startTime() + " - " + endTime() + 
				   ": " + phrase() + " (" + confidence() + ")";
		}
	}
	
	public static Collection<Phrase> createPhrasesFromFile(File liumFile) throws IOException {
		String[] lines = FileUtils.readlines(liumFile);
		
		List<Phrase> phrases = new ArrayList<Phrase>();
		
		for (String line : lines) {
			String[] components = line.split(" ");
			
			Word word = new Word();
			word.start = Float.parseFloat(components[2]);
			word.end = word.start + 0.02f;
			word.word = components[4];
			word.confidence = Float.parseFloat(components[5]);
			
			Phrase phrase = new Phrase();
			phrase.add(word);
			
			phrases.add(phrase);
		}
		
		ClusterToClusterDivergenceFunction<DefaultCluster<Phrase>, Phrase> divFunc = 
        	new ClusterToClusterDivergenceFunction<DefaultCluster<Phrase>, Phrase>() {

				@Override
				public double evaluate(DefaultCluster<Phrase> first,
						DefaultCluster<Phrase> second) {
					Phrase firstPhrase = new Phrase(first.getMembers());
					Phrase secondPhrase = new Phrase(second.getMembers());
					
					/*if (firstPhrase.start < secondPhrase.start) {
						if (firstPhrase.end < secondPhrase.start) {
							return secondPhrase.start - firstPhrase.end;
						} else {
							return 0;
						}
					} else {
						if (secondPhrase.end < firstPhrase.start) {
							return firstPhrase.start - secondPhrase.end;
						} else {
							return 0;
						}
					}*/
					
					return Math.pow(firstPhrase.startTime() - secondPhrase.startTime(), 2) +
						   Math.pow(firstPhrase.endTime() - secondPhrase.endTime(), 2);
				}
				
				public CloneableSerializable clone() {
					return null;
				}
        	
        };
        
        AgglomerativeClusterer<Phrase, DefaultCluster<Phrase>> clusterer = 
        	new AgglomerativeClusterer<Phrase, DefaultCluster<Phrase>>
        		(divFunc, new DefaultClusterCreator<Phrase>(), 1d);
        ClusterHierarchyNode<Phrase, DefaultCluster<Phrase>> root = 
        	clusterer.clusterHierarchically(phrases);
    
        OverlapSortedSet<Phrase, Word> resultSet = new OverlapSortedSet<Phrase, Word>();
        
       	Queue<ClusterHierarchyNode<Phrase, DefaultCluster<Phrase>>> nodeQueue = 
       		new ConcurrentLinkedQueue<ClusterHierarchyNode<Phrase, DefaultCluster<Phrase>>>();
       	nodeQueue.add(root);
       	
       	SortedSet<Phrase> phraseQueue =	new TreeSet<Phrase>();
       	
       	while (!nodeQueue.isEmpty()) {
       		ClusterHierarchyNode<Phrase, DefaultCluster<Phrase>> current = 
       			nodeQueue.remove();
       		
       		Phrase phrase = new Phrase(current.getMembers());
       		
       		phraseQueue.add(phrase);
       		
       		if (current.hasChildren()) {
       			nodeQueue.addAll(current.getChildren());
       		}
       	}
       	
       	resultSet.addAll(phraseQueue);
       	
       	return resultSet;
	}
	
	public static void main(String[] args) throws IOException {
		Collection<Phrase> phrases = createPhrasesFromFile(new File(args[0]));
		
		for (Phrase phrase : phrases) {
			System.out.println(phrase);
		}
	}
}
