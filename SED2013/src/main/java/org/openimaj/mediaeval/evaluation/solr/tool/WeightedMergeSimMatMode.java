package org.openimaj.mediaeval.evaluation.solr.tool;

import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.kohsuke.args4j.Option;
import org.openimaj.io.IOUtils;
import org.openimaj.math.matrix.MatlibMatrixUtils;
import org.openimaj.mediaeval.evaluation.solr.SED2013SolrSimilarityMatrix;
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
	
	@Override
	public void setup() {
		this.perms = perm(2 + extraMergeParts,this.simmat.size()).iterator();
	}
	class HashableIntArr{
		int[] arr;
		@Override
		public int hashCode() {
			return Arrays.hashCode(arr);
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof HashableIntArr && Arrays.equals(this.arr, ((HashableIntArr)obj).arr);
		}
	}
	private List<int[]> perm(int totalSplits, int totalEntries) {
		
		Set<HashableIntArr> hashableRet = new HashSet<HashableIntArr>();
		perm(totalSplits,totalEntries,hashableRet,new TIntArrayStack(totalSplits+2));
		List<int[]> ret = new ArrayList<int[]>();
		for (HashableIntArr hashableDoubleArr : hashableRet) {
			ret.add(hashableDoubleArr.arr);
		}
		return ret;
	}
	private void perm(int totalSplits, int totalEntries, Set<HashableIntArr> toret, TIntStack stack) {
		if(totalEntries == 0){
			HashableIntArr v = new HashableIntArr();
			v.arr = new int[stack.size()];
			stack.toArray(v.arr);
			double sum = 0;
			for (int i = 0; i < v.arr.length; i++) {
				sum += v.arr[i];
			}
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
		super.setup(); // prepare the simmat iterator
		int[] curperm = this.perms.next();
		float curpermSum = ArrayUtils.sumValues(curperm);
		NamedSolrSimilarityMatrixClustererExperiment ret = new NamedSolrSimilarityMatrixClustererExperiment();
		SparseMatrix mat = null;
		String combname = "";
		int i = 0;
		try{					
			while(this.simMatIter.hasNext()){
				String next = this.simMatIter.next();
				SparseMatrix newmat = null;
				float prop = curperm[i]/curpermSum;
				if(this.simmatRoot != null){
					if(prop!=0)
						newmat = SED2013SolrSimilarityMatrix.readSparseMatricies(this.simmatRoot, next).get(next);
					combname += String.format("%s=%2.2f/",next,prop);
				}
				else {
					File nextf = new File(next);
					if(prop!=0)
						newmat = IOUtils.readFromFile(nextf);
					combname += String.format("%s=%2.2f/",nextf.getName(),prop);
				}
				if(newmat!=null)
				{
					MatlibMatrixUtils.scaleInplace(newmat, prop);
					if(mat == null){
						mat = newmat;
					}
					else{
						MatlibMatrixUtils.plusInplace(mat, newmat);
					}
				}
				i++;
			}
		} catch(Exception e){
			throw new RuntimeException(e);
		}
		
		ret.exp = new SolrSimilarityExperimentTool(combname, mat, index, this.start, this.end);
		ret.name = String.format("nmats=%d/%s",curperm.length,combname);
		
		return ret;
	}

}
