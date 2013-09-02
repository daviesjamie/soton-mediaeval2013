package uk.ac.soton.ecs.jsh2.mediaeval13.diversity;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.openimaj.util.pair.ObjectDoublePair;

import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.Diversifier;
import uk.ac.soton.ecs.jsh2.mediaeval13.diversity.diversification.RandomDiversifier;

public class DiversificationHarness {
	public static void performDiversification(File baseDir, boolean devset, Diversifier diversifier, File output,
			String runId)
			throws Exception
	{
		final PrintWriter pw;
		PrintWriter htmlPw = null;

		if (output == null) {
			pw = new PrintWriter(System.out);
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

		for (final ResultList rl : ResultList.loadResultSet(baseDir, devset)) {
			System.err.println("Diversifying " + rl.monument);

			final List<ObjectDoublePair<ResultItem>> results = diversifier.diversify(rl);

			formatResults(results, rl, runId, pw);

			if (htmlPw != null)
				printHtml(results, rl, runId, htmlPw);
		}

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
			// perform the evaluation by running the shell script
			final Process p = Runtime.getRuntime().exec("./run-eval.sh " + output.getAbsolutePath(), null, baseDir);
			p.waitFor();

			System.out.println(
					FileUtils.readFileToString(new File(output.getParent(), output.getName() + "_metrics.csv")));
		}
	}

	private static void printHtml(List<ObjectDoublePair<ResultItem>> results, ResultList rl, String runId,
			PrintWriter htmlPw)
	{
		htmlPw.format("<h1>%s</h1>\n", rl.monument);
		for (int i = 0; i < 10; i++) {
			final File fn = results.get(i).first.getImageFile().getAbsoluteFile();
			htmlPw.format("<a href=\"file://%s\"><img src=\"file://%s\" width='100'/></a>\n", fn, fn);
		}
		htmlPw.println("<hr/>");
	}

	private static void formatResults(List<ObjectDoublePair<ResultItem>> results, ResultList rl, String runId,
			PrintWriter pw)
	{
		final String qid = String.format("%03d", rl.number);
		final String iter = "0";

		for (int rnk = 0; rnk < Math.min(results.size(), 50); rnk++) {
			final double sim = results.get(rnk).second;
			final ResultItem item = results.get(rnk).first;

			pw.format("%s %s %d %d %2.4f %s\n", qid, iter, item.id, rnk, sim, runId);
		}
	}

	public static void main(String[] args) throws Exception {
		final File baseDir = new File("/Users/jon/Data/mediaeval/diversity/");
		final boolean devset = true;
		final Diversifier diversifier = new RandomDiversifier();
		final String runId = "random-top50";

		final File output = new File(baseDir, "experiments/" + runId);
		performDiversification(baseDir, devset, diversifier, output, runId);
	}
}
