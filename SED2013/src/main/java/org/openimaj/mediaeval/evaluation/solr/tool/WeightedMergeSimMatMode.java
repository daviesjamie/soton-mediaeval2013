package org.openimaj.mediaeval.evaluation.solr.tool;

import gnu.trove.stack.TDoubleStack;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TDoubleArrayStack;
import gnu.trove.stack.array.TIntArrayStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Option;
import org.mortbay.io.RuntimeIOException;
import org.openimaj.mediaeval.evaluation.solr.SimilarityMatrixWrapper;
import org.openimaj.mediaeval.evaluation.solr.SolrSimilarityMatrixClustererExperiment.SparseMatrixSource;
import org.openimaj.mediaeval.evaluation.solr.tool.IncrementalWeightedMergeMode.HashableIntArr;
import org.openimaj.util.array.ArrayUtils;

/**
 *
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class WeightedMergeSimMatMode extends SimMatSetupMode {
	
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--weighted-merge-parts", 
		aliases="-wmp", 
		required=false, 
		usage="The number of extra weightings other than 0 and 1 to give to a similarity matrix. Defaults to 0, meaning only on or off. if set to 3: 0, 0.25,0.5,0.75 and 1 will be explored", 
		metaVar="DOUBLE"
	)
	public int extraMergeParts = 0;
	private Iterator<double[]> weightPermutations;
	
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--weighted-merge-permutations", 
		aliases="-wmperm", 
		required=false, 
		usage="The permutations of weights, overrides the grid search",
		multiValued=true
	)
	public List<String> permStrings = null;
	
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--weighted-merge-permutations-file", 
		aliases="-wmpermf", 
		required=false, 
		usage="A file containing one permutation of weights per line, overrides the grid search,",
		multiValued=true
	)
	public String permStringsFile = null;
	
	/**
	 * The eps to start to search
	 */
	@Option(
		name="--weighted-merge-combination-mode", 
		aliases="-wmcm", 
		required=false, 
		usage="How to combine matricies"
	)
	public WeightedMergeCombinationMode wmcm = WeightedMergeCombinationMode.SUM;
	private Map<String, SimilarityMatrixWrapper> allmats;
	
	
	@Override
	public void setup() {
		super.setup();
		try {
			this.allmats = super.readSparseMatricies(simmatRoot,this.simmat.toArray(new String[this.simmat.size()]));
			simmat = new ArrayList<String>();
			simmat.addAll(this.allmats.keySet());
			
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		if(this.permStrings != null){
			List<double[]> permIntList = new ArrayList<double[]>();
			for (String permString : this.permStrings) {
				String[] permSplits = permString.split(",");
				double[] permints = new double[permSplits.length];
				double sum = 0;
				for (int i = 0; i < permSplits.length; i++) {
					sum += Double.parseDouble(permSplits[i]);
					permints[i] = Double.parseDouble(permSplits[i]);
				}
				if(sum == 0) continue;
				permIntList.add(permints);
			}
			this.weightPermutations = permIntList.iterator();
		}
		else{			
			this.weightPermutations = perm(2 + extraMergeParts,this.simmat.size()).iterator();
		}
		
	}
	class HashableIntArr{
		private double[] arr;
		private double[] proped;
		public HashableIntArr(double[] arr) {
			this.arr = arr;
			proped = new double[arr.length];
			double sum = ArrayUtils.sumValues(arr);
			if(sum == 0) return;
			for (int i = 0; i < proped.length; i++) {
				proped[i] = arr[i] / sum;
			}
		}
		@Override
		public int hashCode() {
			int hashCode = Arrays.hashCode(proped);
			return hashCode;
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof HashableIntArr && Arrays.equals(this.proped, ((HashableIntArr)obj).proped);
		}
		
		@Override
		public String toString() {
			return Arrays.toString(this.arr);
		}
	}
	private Collection<double[]> perm(int totalSplits, int totalEntries) {
		
		Set<HashableIntArr> hashableRet = new HashSet<HashableIntArr>();
		perm(totalSplits,totalEntries,hashableRet,new TDoubleArrayStack(totalSplits+2));
		Set<double[]> ret = new TreeSet<double[]>(new Comparator<double[]>() {
			@Override
			public int compare(double[] o1, double[] o2) {
				for (int i = 0; i < o2.length; i++) {
					int cmpret = Double.compare(o1[i], o2[i]);
					if(cmpret == 0) continue;
					return cmpret;
				}
				return 0;
			}
		});
		for (HashableIntArr hashableDoubleArr : hashableRet) {
			ret.add(hashableDoubleArr.arr);
		}
		return ret;
	}
	private void perm(int totalSplits, int totalEntries, Set<HashableIntArr> toret, TDoubleStack stack) {
		if(totalEntries == 0){
			double[] arr = new double[stack.size()];
			stack.toArray(arr);
			HashableIntArr v = new HashableIntArr(arr);
			double sum = ArrayUtils.sumValues(v.proped);
			if(sum==0) return;
			
			toret.add(v);
			return;
		}
		for (int i = 0; i < totalSplits; i++) {
			stack.push(i);
			perm(totalSplits,totalEntries-1,toret,stack);
			stack.pop();
		}
	}
	@Override
	public boolean hasNextSimmat() {
		return weightPermutations.hasNext();
	}

	@Override
	public NamedSolrSimilarityMatrixClustererExperiment nextSimmat() {
		
		final double[] curperm = weightPermutations.next();
		final String combname = prepareName(curperm);
		
		SparseMatrixSource sps = new SparseMatrixSource() {
			
			@Override
			public String name() {
				return combname;
			}
			
			@Override
			public SimilarityMatrixWrapper mat() {
				WeightedMergeSimMatMode.super.setup(); // prepare the simmat iterator
				int i = 0;
				SimilarityMatrixWrapper[] tocombine = new SimilarityMatrixWrapper[curperm.length];
				try{					
					while(simMatIter.hasNext()){
						tocombine[i] = allmats.get(simMatIter.next());
						i++;
					}
					SimilarityMatrixWrapper matwrapper= wmcm.combine(tocombine, curperm);
					return matwrapper;
				} catch(Exception e){
					throw new RuntimeException(e);
				}
			}
		};
		NamedSolrSimilarityMatrixClustererExperiment ret = new NamedSolrSimilarityMatrixClustererExperiment();
		ret.exp = new SolrSimilarityExperimentTool(sps, index);
		ret.name = String.format("combine=%s/%s",wmcm.name(),combname);
		
		return ret;
	}
	private String prepareName(double[] curperm) {
		WeightedMergeSimMatMode.super.setup(); // prepare the simmat iterator
		
		String combname = "";
		int i = 0;					
		while(simMatIter.hasNext()){
			String next = simMatIter.next();
			if(simmatRoot != null){
				combname += String.format("%s=%2.5f/",next,curperm[i]);
			}
			else {
				File nextf = new File(next);
				combname += String.format("%s=%2.5f/",nextf.getName(),curperm[i]);
			}
			i++;
		}
		return combname;
	}

}
