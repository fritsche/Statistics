package br.ufpr.inf.cbio.statistics;

import java.io.BufferedReader;

//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU Lesser General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Lesser General Public License for more details.
//
//You should have received a copy of the GNU Lesser General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 *
 * @return matrix alg x alg a cell is true if there is statistical difference
 *         between the alg_column and the alg_line false otherwise
 *
 */
public class FriedmanTest {

        public static double confidence = 0.99;
    
	public static HashMap<String, HashMap<String, Boolean>> test(HashMap<String, double[]> values, String outputDir)
			throws IOException, InterruptedException {

		String script = "require(PMCMR)\noptions(\"width\"=10000)\n";
		script += "ARRAY <- c(";
		int size = 0;
		for (Map.Entry<String, double[]> entrySet : values.entrySet()) {
			double[] keyValues = entrySet.getValue();
			size = keyValues.length;

			for (Double value : keyValues) {
				script += value + ",";
			}
		}
		script = script.substring(0, script.lastIndexOf(",")) + ")";
		script += "\n";

		script += "categs<-as.factor(rep(c(";
		for (Map.Entry<String, double[]> entrySet : values.entrySet()) {
			String key = entrySet.getKey();
			script += "\"" + key + "\",";
		}
		script = script.substring(0, script.lastIndexOf(","));
		script += "),each=" + size + "))";
		script += "\n";

		script += "probs<-as.factor(rep(c(";
		for (int i = 0; i < size; ++i) {
			script += "\"" + i + "\",";
		}
		script = script.substring(0, script.lastIndexOf(","));
		script += ")," + values.size() + "))";
		script += "\n";

		script += "result <- friedman.test(ARRAY,categs,probs)\n";
		script += "print(result);\n";
		script += "pos_teste<-posthoc.friedman.nemenyi.test(ARRAY, categs, probs, method='Tukey');\n";
		script += "print(pos_teste)\n";

		StatisticalTests.checkDirectory(outputDir);
		File scriptFile = new File(outputDir + "/friedmanscript.R");
		File outputFile = new File(outputDir + "/friedmanoutput.R");

		try (FileWriter scriptWriter = new FileWriter(scriptFile)) {
			scriptWriter.append(script);
		}

		ProcessBuilder processBuilder = new ProcessBuilder("R", "--slave", "-f", scriptFile.getAbsolutePath());
		processBuilder.redirectOutput(outputFile);

		Process process = processBuilder.start();
		process.waitFor();
		if (process.exitValue() != 0) {
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String line;
			while ((line = br.readLine()) != null) {
				System.err.println(line);
			}
			throw new InterruptedException("R process failed! Check if R is installed");
		}

		ArrayList<Map.Entry<String, double[]>> entrySets = new ArrayList<>(values.entrySet());

		HashMap<String, HashMap<String, Boolean>> result = new HashMap<>();
		HashMap<String, HashMap<String, Double>> matrix = new HashMap<>();

		int combinacoes = values.size();
		ArrayList<String> lines = new ArrayList<String>();
		Scanner scanner = new Scanner(outputFile);
		while (scanner.hasNextLine()) {
			lines.add(scanner.nextLine());
		}
		for (int i = lines.size() - combinacoes - 1; i < lines.size() - 2; i++) {
			double[] splittedValue = new double[combinacoes];

			for (int j = 0; j < combinacoes - 1; j++) {

				String part = lines.get(i).replace("<", "").split("\\s+")[j + 1];

				if (part.compareTo("-") != 0) {

					String l = lines.get(i).split("\\s+")[0];
					String c = lines.get(lines.size() - combinacoes - 2).split("\\s+")[j + 1];

					matrix.put(l, new HashMap<>());
					matrix.get(l).put(c, Double.parseDouble(part));

					matrix.put(c, new HashMap<>());
					matrix.get(c).put(l, Double.parseDouble(part));
				}
			}
		}

		for (Map.Entry<String, HashMap<String, Double>> entry : matrix.entrySet()) {
			String key = entry.getKey();
			HashMap<String, Double> value = entry.getValue();
			for (Map.Entry<String, Double> entry2 : value.entrySet()) {
				String key2 = entry2.getKey();
				double dvalue = entry2.getValue();
				result.put(key, new HashMap<String, Boolean>());
				if (dvalue < (1-confidence)) {
					result.get(key).put(key2, true);
				} else {
					result.get(key).put(key2, false);
				}
			}
		}

		return result;

	}

}
