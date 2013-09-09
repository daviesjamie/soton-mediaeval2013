package uk.ac.soton.ecs.jsh2.mediaeval13.diversity;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.scoring.Scorer;

public class DiversificationHarness {
	public static void performDiversification(File baseDir, boolean devset, final Scorer diversifier, File output,
			final String runId)
			throws Exception
	{
		final PrintWriter pw;
		final PrintWriter htmlPw;

		if (output == null) {
			pw = new PrintWriter(System.out);
			htmlPw = null;
		} else {
			output.getParentFile().mkdirs();
			pw = new PrintWriter(new FileWriter(output));
			htmlPw = new PrintWriter(new FileWriter(new File(output.getParentFile(), output.getName() + ".html")));
		}

		if (htmlPw != null) {
			htmlPw.println("<html>");
			htmlPw.format("<head><title>%s</title></head>\n", runId);
			htmlPw.println("<body>");
		}

		final Iterable<ResultList> allLists = ResultList.loadResultSet(baseDir, devset);
		for (final ResultList rl : allLists) {
			System.err.println("Diversifying " + rl.monument);

			final List<ObjectDoublePair<ResultItem>> results =
					diversifier.score(rl);

			formatResults(results, rl, runId, pw);

			if (htmlPw != null)
				printHtml(results, rl, runId, htmlPw);
		}

		// Parallel.forEach(allLists, new Operation<ResultList>() {
		// @Override
		// public void perform(ResultList rl) {
		// System.err.println("Diversifying " + rl.monument);
		//
		// final List<ObjectDoublePair<ResultItem>> results =
		// diversifier.score(rl);
		//
		// formatResults(results, rl, runId, pw);
		//
		// if (htmlPw != null) {
		// printHtml(results, rl, runId, htmlPw);
		// }
		// }
		// });

		if (htmlPw != null) {
			htmlPw.println("</body>");
			htmlPw.println("</html>");
		}

		pw.flush();
		if (output != null) {
			pw.close();
			htmlPw.close();
		}

		if (output != null && devset) {
			final File metricsFile = new File(output.getParent(), output.getName() + "_metrics.csv");
			metricsFile.delete();

			// perform the evaluation by running the shell script
			final Process p = Runtime.getRuntime().exec("./run-eval.sh " + output.getAbsolutePath(), null, baseDir);
			p.waitFor();

			System.out.println(
					FileUtils.readFileToString(metricsFile));
		}
	}

	private static void printHtml(List<ObjectDoublePair<ResultItem>> results, ResultList rl, String runId,
			PrintWriter htmlPw)
	{
		htmlPw.format("<h1>%s</h1>\n", rl.monument);
		for (int i = 0; i < Math.min(10, results.size()); i++) {
			final File fn = results.get(i).first.getImageFile().getAbsoluteFile();
			htmlPw.format("<a href=\"file://%s\"><img src=\"file://%s\" width='100'/></a>\n", fn, fn);
		}
		htmlPw.println("<hr/>");
	}

	private static void formatResults(List<ObjectDoublePair<ResultItem>> results, ResultList rl, String runId,
			PrintWriter pw)
	{
		final String qid = String.format("%d", rl.number);
		final String iter = "0";

		for (int rnk = 0; rnk < Math.min(results.size(), 50); rnk++) {
			final double sim = results.get(rnk).second;
			final ResultItem item = results.get(rnk).first;

			pw.format("%s %s %d %d %2.4f %s\n", qid, iter, item.id, rnk, sim, runId);
		}
	}
}
