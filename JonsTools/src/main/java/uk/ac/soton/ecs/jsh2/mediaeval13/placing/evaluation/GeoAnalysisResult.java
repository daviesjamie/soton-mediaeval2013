package uk.ac.soton.ecs.jsh2.mediaeval13.placing.evaluation;

import java.util.List;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;

import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.ranking.NaNStrategy;
import org.apache.commons.math.stat.ranking.NaturalRanking;
import org.apache.commons.math.stat.ranking.TiesStrategy;
import org.openimaj.experiment.evaluation.AnalysisResult;
import org.openimaj.util.pair.DoubleDoublePair;

import com.bethecoder.ascii_table.ASCIITable;
import com.bethecoder.ascii_table.ASCIITableHeader;

/**
 * The results of analysing a set of predicted versus actual geo locations.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class GeoAnalysisResult implements AnalysisResult {
	private static int[] distances = { 1, 10, 100, 1000 };

	private List<DoubleDoublePair> results;

	private double[] accuracyPerDistance = new double[distances.length];
	private double medianError;
	private double linearCorrelation;
	private double kendallTauCorrelation;

	public GeoAnalysisResult(List<DoubleDoublePair> results) {
		this.results = results;
		compute();
	}

	private void compute() {
		final double[] actualError = new double[results.size()];
		final double[] predictedError = new double[results.size()];

		final DescriptiveStatistics stats = new DescriptiveStatistics();

		for (int i = 0; i < actualError.length; i++) {
			final DoubleDoublePair r = results.get(i);

			for (int j = 0; j < distances.length; j++) {
				if (r.first < distances[j])
					accuracyPerDistance[j]++;
			}

			actualError[i] = r.first;
			predictedError[i] = r.second;

			stats.addValue(actualError[i]);
		}

		medianError = stats.getPercentile(50);

		for (int j = 0; j < distances.length; j++) {
			accuracyPerDistance[j] /= results.size();
		}

		linearCorrelation = new PearsonsCorrelation().correlation(actualError, predictedError);
		kendallTauCorrelation = computeKendallTau(actualError, predictedError);
	}

	private static double computeKendallTau(double[] x, double[] y) {
		final NaturalRanking ranking = new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.AVERAGE);
		final double[] q = ranking.rank(x);
		final double[] p = ranking.rank(y);

		int numer = 0;
		final int n = x.length;
		for (int i = 1; i < n; i++) {
			for (int j = 0; j < i; j++) {
				numer += sgn(q[i] - q[j]) * sgn(p[i] - p[j]);
			}
		}

		return numer / (0.5 * n * (n - 1));
	}

	private static final int sgn(double x) {
		return x == 0 ? 0 : x < 0 ? -1 : 1;
	}

	@Override
	public JasperPrint getSummaryReport(String title, String info) throws JRException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JasperPrint getDetailReport(String title, String info) throws JRException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSummaryReport() {
		return getDetailReport();
	}

	@Override
	public String getDetailReport() {
		final String[][] table = new String[accuracyPerDistance.length + 3][2];

		for (int i = 0; i < accuracyPerDistance.length; i++) {
			table[i][0] = "Percentage accuracy within " + distances[i] + "km";
			table[i][1] = "" + accuracyPerDistance[i];
		}
		table[0 + accuracyPerDistance.length][0] = "Median Error";
		table[0 + accuracyPerDistance.length][1] = "" + medianError;

		table[1 + accuracyPerDistance.length][0] = "Linear Correlation";
		table[1 + accuracyPerDistance.length][1] = "" + linearCorrelation;

		table[2 + accuracyPerDistance.length][0] = "Kendall's Tau";
		table[2 + accuracyPerDistance.length][1] = "" + kendallTauCorrelation;

		final ASCIITableHeader[] header = {
				new ASCIITableHeader("Statistic", ASCIITable.ALIGN_RIGHT),
				new ASCIITableHeader("Value", ASCIITable.ALIGN_LEFT)
		};

		return ASCIITable.getInstance().getTable(header, table);
	}

	@Override
	public String toString() {
		return getDetailReport();
	}
}
