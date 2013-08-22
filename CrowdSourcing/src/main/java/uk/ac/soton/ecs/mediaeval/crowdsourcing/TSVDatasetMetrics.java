package uk.ac.soton.ecs.mediaeval.crowdsourcing;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.openimaj.image.annotation.evaluation.agreement.CohensKappaInterraterAgreement;
import org.openimaj.ml.annotation.ScoredAnnotation;
import org.openimaj.util.iterator.TextLineIterable;

import com.bethecoder.ascii_table.ASCIITable;
import com.bethecoder.ascii_table.ASCIITableHeader;

/**
 * 
 * 
 * @author David Dupplaw (dpd@ecs.soton.ac.uk)
 * @created 22 Aug 2013
 * @version $Author$, $Revision$, $Date$
 */
public class TSVDatasetMetrics
{
	public static enum ValueComparator
	{
		YESNO
		{
			@Override
			public boolean isTrue(final String str)
			{
				return str.toLowerCase().equals("yes");
			}
		};

		public abstract boolean isTrue(String str);
	}

	/**
	 * 
	 * @author David Dupplaw (dpd@ecs.soton.ac.uk)
	 * @created 22 Aug 2013
	 * @version $Author$, $Revision$, $Date$
	 */
	public static class Options
	{
		/** Whether there's a single dataset per file */
		public boolean datasetPerFile = true;

		@Option(name = "-k", aliases = "-keyColumn", usage = "Column in which the join key is held")
		public int keyColumn = 1;

		@Option(name = "-d", aliases = "-dataColumn", usage = "Column in which the data key is held")
		public int dataColumn = 2;

		/** The input files that are to be input into the face tools */
		@Argument(required = true, usage = "Data files", metaVar = "FILES")
		public List<File> inputFiles = new ArrayList<File>();

		public boolean cohensKappa = false;

		public boolean F1measure = true;

		public boolean precision = true;

		public boolean recall = true;

		public boolean missFirstLine = false;

		public ValueComparator comparator = ValueComparator.YESNO;
	}

	private final Options options;

	/**
	 * Default constructor
	 * 
	 * @param options
	 *            The options
	 */
	public TSVDatasetMetrics(final Options options)
	{
		this.options = options;
		final List<Map<String, ScoredAnnotation<String>>> x = this.parseFiles(options.inputFiles);
		this.makeOutput(x);
	}

	/**
	 * Output
	 * 
	 * @param x
	 *            The datasets
	 */
	private void makeOutput(final List<Map<String, ScoredAnnotation<String>>> x)
	{
		if (this.options.cohensKappa)
		{
			if (x.size() < 2)
				System.err.println("Cohen's Kappa requires two datasets - one for each rater");
			else
			{
				if (x.size() > 2)
					System.err.println("WARNING: Using only the first two datasets for Cohen's Kappa");

				final Map<String, ScoredAnnotation<String>> rater1 = x.get(0);
				final Map<String, ScoredAnnotation<String>> rater2 = x.get(1);

				final double k = CohensKappaInterraterAgreement.calculate(rater1, rater2);
				System.out.println("Cohen's Kappa: " + k);
			}
		}

		if (this.options.precision)
		{
			if (x.size() < 2)
				System.out.println("Cohen's Kappa requires two datasets - one for the ground truth and one for the test");
			else
			{
				final Map<String, ScoredAnnotation<String>> gt = x.get(0);
				final Map<String, ScoredAnnotation<String>> testset = x.get(1);

				// System.out.println("Ground truth size: " + gt.size());
				// System.out.println("Test set size: " + testset.size());

				int missingInTestSet = 0;
				int tp = 0;
				int fp = 0;
				int fn = 0;
				for (final String k : gt.keySet())
				{
					final ScoredAnnotation<String> gtval = gt.get(k);
					final ScoredAnnotation<String> testval = testset.get(k);

					if (testval == null)
						missingInTestSet++;
					else
					{
						final boolean gtt = this.options.comparator.isTrue(gtval.annotation);
						final boolean tst = this.options.comparator.isTrue(testval.annotation);

						// True positive
						if (gtt && tst)
							tp++;

						// False positive
						if (tst && !gtt)
							fp++;

						// False negative
						if (!tst && gtt)
							fn++;
					}
				}

				final double prec = (double) tp / (double) (tp + fp);
				final double recall = (double) tp / (double) (tp + fn);
				final double F1 = (2 * prec * recall) / (prec + recall);

				final String[][] table = {
						{ "True positives: ", tp + "" },
						{ "False positives: ", fp + "" },
						{ "False negatives: ", fn + "" },
						{ "Precision: ", prec + "" },
						{ "Recall: ", recall + "" },
						{ "F1: ", F1 + "" } };

				final ASCIITableHeader[] header = {
						new ASCIITableHeader("Statistic", ASCIITable.ALIGN_RIGHT),
						new ASCIITableHeader("Value", ASCIITable.ALIGN_LEFT)
				};

				System.out.println(ASCIITable.getInstance().getTable(header, table));

				if (missingInTestSet > 0)
					System.err.println("Missing from test set: " + missingInTestSet);
			}
		}
	}

	/**
	 * Parse all the files into memory datasets
	 * 
	 * @param input
	 *            The list of files
	 * @return
	 */
	private List<Map<String, ScoredAnnotation<String>>> parseFiles(final List<File> input)
	{
		final List<Map<String, ScoredAnnotation<String>>> result =
				new ArrayList<Map<String, ScoredAnnotation<String>>>();
		for (final File f : input)
			result.add(this.parseFile(f));
		return result;
	}

	/**
	 * Parse a single file into an annotated dataset
	 * 
	 * @param f
	 *            The file to parse
	 * @return The dataset
	 */
	private Map<String, ScoredAnnotation<String>> parseFile(final File f)
	{
		final Map<String, ScoredAnnotation<String>> result = new HashMap<String, ScoredAnnotation<String>>();
		boolean firstLine = true;
		for (final String line : new TextLineIterable(f))
		{
			if (this.options.missFirstLine && firstLine)
			{
				firstLine = false;
				continue;
			}

			final String[] parts = line.split("\t");

			result.put(parts[this.options.keyColumn - 1],
					new ScoredAnnotation<String>(parts[this.options.dataColumn - 1], 1)
					);
		}
		return result;
	}

	/**
	 * Parses the command line arguments.
	 * 
	 * @param args
	 *            The arguments to parse
	 * @return The tool options class
	 */
	private static Options parseArgs(final String[] args)
	{
		final Options fdto = new Options();
		final CmdLineParser parser = new CmdLineParser(fdto);

		try
		{
			parser.parseArgument(args);
		} catch (final CmdLineException e)
		{
			System.err.println(e.getMessage());
			System.err.println("java TSVDatasetMetricsTool [options...] FILES");
			parser.printUsage(System.err);
			return null;
		}

		return fdto;
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args)
	{
		final Options o = TSVDatasetMetrics.parseArgs(args);

		if (o != null)
			new TSVDatasetMetrics(o);
	}
}
