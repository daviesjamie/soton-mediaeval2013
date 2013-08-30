package org.openimaj.mediaeval.evaluation.solr.tool;

import gnu.trove.stack.TIntStack;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.kohsuke.args4j.Option;
import org.mortbay.io.RuntimeIOException;
import org.openimaj.io.IOUtils;
import org.openimaj.math.matrix.MatlibMatrixUtils;
import org.openimaj.mediaeval.evaluation.solr.SED2013SolrSimilarityMatrix;
import org.openimaj.mediaeval.evaluation.solr.SimilarityMatrixWrapper;
import org.openimaj.mediaeval.evaluation.solr.SolrSimilarityMatrixClustererExperiment.SparseMatrixSource;
import org.openimaj.util.array.ArrayUtils;

import ch.akuhn.matrix.SparseMatrix;

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
	private Iterator<int[]> perms;
	
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
		name="--weighted-merge-combination-mode", 
		aliases="-wmcm", 
		required=false, 
		usage="How to combine matricies"
	)
	public WeightedMergeCombinationMode wmcm = WeightedMergeCombinationMode.SUM;
	private Map<String, SimilarityMatrixWrapper> allmats;
	
	
	@Override
	public void setup() {
		try {
			if(simmatRoot!=null)
			{
				Map<String, SparseMatrix> allmatsFull = SED2013SolrSimilarityMatrix.readSparseMatricies(simmatRoot);
				this.allmats = new HashMap<String, SimilarityMatrixWrapper>();
				for (Entry<String, SparseMatrix> ent : allmatsFull.entrySet()) {
					this.allmats.put(ent.getKey(), new SimilarityMatrixWrapper(ent.getValue(), start, end));
				}
			}
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		if(this.permStrings != null){
			List<int[]> permIntList = new ArrayList<int[]>();
			for (String permString : this.permStrings) {
				// This code turns something like: 0.5,0.5,0.5
				// into: 1,1,1
				// and 0.5,7,0 into: 0,2,0
				// a list of ints will be mostly untransformed
				String[] permSplits = permString.split(",");
				double[] permdoubles = new double[permSplits.length];
				double sum = 0;
				for (int i = 0; i < permSplits.length; i++) {
					permdoubles[i] = Double.parseDouble(permSplits[i]);
					sum += permdoubles[i];
				}
				if(sum == 0) continue;
				int[] permints = new int[permdoubles.length];
				for (int i = 0; i < permints.length; i++) {
					permints[i] = (int) ((permdoubles[i]/sum) * permdoubles.length);
				}
				permIntList.add(permints);
			}
			this.perms = permIntList.iterator();
		}
		else{			
			this.perms = perm(2 + extraMergeParts,this.simmat.size()).iterator();
		}
		
	}
	class HashableIntArr{
		private int[] arr;
		private double[] proped;
		public HashableIntArr(int[] arr) {
			this.arr = arr;
			proped = new double[arr.length];
			float sum = ArrayUtils.sumValues(arr);
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
	private Collection<int[]> perm(int totalSplits, int totalEntries) {
		
		Set<HashableIntArr> hashableRet = new HashSet<HashableIntArr>();
		perm(totalSplits,totalEntries,hashableRet,new TIntArrayStack(totalSplits+2));
		Set<int[]> ret = new TreeSet<int[]>(new Comparator<int[]>() {
			@Override
			public int compare(int[] o1, int[] o2) {
				for (int i = 0; i < o2.length; i++) {
					int cmpret = Integer.compare(o1[i], o2[i]);
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
	private void perm(int totalSplits, int totalEntries, Set<HashableIntArr> toret, TIntStack stack) {
		if(totalEntries == 0){
			int[] arr = new int[stack.size()];
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
		return perms.hasNext();
	}

	@Override
	public NamedSolrSimilarityMatrixClustererExperiment nextSimmat() {
		
		final int[] curperm = perms.next();
		final String combname = prepareName(curperm);
		
		SparseMatrixSource sps = new SparseMatrixSource() {
			
			@Override
			public String name() {
				return combname;
			}
			
			@Override
			public SimilarityMatrixWrapper mat() {
				WeightedMergeSimMatMode.super.setup(); // prepare the simmat iterator
				float curpermSum = ArrayUtils.sumValues(curperm);
				
				
				int i = 0;
				SparseMatrix[] tocombine = new SparseMatrix[curperm.length];
				try{					
					while(simMatIter.hasNext()){
						String next = simMatIter.next();
						SimilarityMatrixWrapper newmat = null;
						if(simmatRoot != null){
							newmat = allmats.get(next);
						}
						else {
							File nextf = new File(next);
							newmat = new SimilarityMatrixWrapper((SparseMatrix)IOUtils.readFromFile(nextf), start, end);
						}
						tocombine[i] = newmat.matrix();
						i++;
					}
					SparseMatrix mat = wmcm.combine(tocombine, curperm);
					SimilarityMatrixWrapper matwrapper = new SimilarityMatrixWrapper(mat, start, end);
//					compareToAggr(matwrapper);
					return matwrapper;
				} catch(Exception e){
					throw new RuntimeException(e);
				}
			}

			private void compareToAggr(SimilarityMatrixWrapper matwrapper) {
				SparseMatrix aggr = allmats.get("aggregationMean.mat").matrix();
				SimilarityMatrixWrapper aggrwrapper = new SimilarityMatrixWrapper(aggr, start, end);
				aggr = aggrwrapper.matrix();
				for (int r = 0; r < aggr.rowCount(); r++) {
					for (int c = 0; c < aggr.columnCount(); c++) {
						double vrc = aggr.get(r, c);
						if(vrc > 0){
							double matvrc = matwrapper.matrix().get(r, c);
							double d = Math.abs(vrc - matvrc);
							if(d > 0.1){
								System.out.println("Big difference: " + d);
							};
						}
					}
				}
			}
		};
		NamedSolrSimilarityMatrixClustererExperiment ret = new NamedSolrSimilarityMatrixClustererExperiment();
		ret.exp = new SolrSimilarityExperimentTool(sps, index);
		ret.name = String.format("combine=%s/nmats=%d/%s",wmcm.name(),curperm.length,combname);
		
		return ret;
	}
	private String prepareName(int[] curperm) {
		WeightedMergeSimMatMode.super.setup(); // prepare the simmat iterator
		
		String combname = "";
		int i = 0;					
		while(simMatIter.hasNext()){
			String next = simMatIter.next();
			if(simmatRoot != null){
				combname += String.format("%s=%d/",next,curperm[i]);
			}
			else {
				File nextf = new File(next);
				combname += String.format("%s=%d/",nextf.getName(),curperm[i]);
			}
			i++;
		}
		return combname;
	}

}
