package org.openimaj.mediaeval.evaluation.solr.tool;

import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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
		name="--weighted-merge-combination-mode", 
		aliases="-wmcm", 
		required=false, 
		usage="How to combine matricies"
	)
	public WeightedMergeCombinationMode wmcm = WeightedMergeCombinationMode.SUM;
	private Map<String, SparseMatrix> allmats;
	
	
	@Override
	public void setup() {
		try {
			this.allmats = SED2013SolrSimilarityMatrix.readSparseMatricies(simmatRoot);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		this.perms = perm(2 + extraMergeParts,this.simmat.size()).iterator();
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
				
				SparseMatrix mat = null;
				int i = 0;
				try{					
					while(simMatIter.hasNext()){
						String next = simMatIter.next();
						SparseMatrix newmat = null;
						float prop = curperm[i]/curpermSum;
						if(simmatRoot != null){
							if(prop!=0)
								newmat = allmats.get(next);
						}
						else {
							File nextf = new File(next);
							if(prop!=0)
								newmat = IOUtils.readFromFile(nextf);
						}
						if(newmat!=null)
						{
							MatlibMatrixUtils.scaleInplace(newmat, prop);
							if(mat == null){
								mat = MatlibMatrixUtils.subMatrix(newmat, 0,newmat.rowCount(), 0,newmat.columnCount());
							}
							else{
								mat = wmcm.combine(mat, newmat);
							}
						}
						i++;
					}
					return new SimilarityMatrixWrapper(mat, start, end);
				} catch(Exception e){
					throw new RuntimeException(e);
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
