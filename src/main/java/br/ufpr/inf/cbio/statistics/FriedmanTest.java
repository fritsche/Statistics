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

		String script = "require(pgirmess)\n";
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
        for (int i=0; i<size; ++i) {
            script += "\"" + i + "\",";
        }
        script = script.substring(0, script.lastIndexOf(","));
        script += ")," + values.size() + "))";
        script += "\n";

        script += "result <- friedman.test(ARRAY,categs,probs)\n";
        script += "m <- data.frame(result$statistic,result$p.value)\n";
        script += "pos_teste <- friedmanmc(ARRAY,categs,probs)\n";
        script += "print(pos_teste)\n";
        script += "print(m)";

        //File scriptFile = File.createTempFile("script", ".R");
        //File outputFile = File.createTempFile("output", ".R");
        // scriptFile.deleteOnExit();
        // outputFile.deleteOnExit();
        File scriptFile = new File("script.R");
        scriptFile.createNewFile();
        File outputFile = new File(outputDir+"/friedmanOutput.R");
        outputFile.createNewFile();

        try (FileWriter scriptWriter = new FileWriter(scriptFile)) {
            scriptWriter.append(script);
        }

        ProcessBuilder processBuilder = new ProcessBuilder("R", "--slave", "-f", scriptFile.getAbsolutePath());
        processBuilder.redirectOutput(outputFile);

        Process process = processBuilder.start();
        process.waitFor();
        if (process.exitValue() != 0){
            throw new InterruptedException("R process failed! Check if R is installed");
        }

        ArrayList<Map.Entry<String, double[]>> entrySets = new ArrayList<>(values.entrySet());

        HashMap<String, HashMap<String, Boolean>> result = new HashMap<>();

        for (int i = 0; i < entrySets.size() - 1; i++) {
            String entry1 = entrySets.get(i).getKey();
            for (int j = i + 1; j < entrySets.size(); j++) {
                String entry2 = entrySets.get(j).getKey();

                Scanner scanner = new Scanner(outputFile);

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    // System.out.println(line);
                    if (line.contains(entry1 + "-" + entry2)
                            || line.contains(entry2 + "-" + entry1)) {
                        HashMap<String, Boolean> entry1Map = result.get(entry1);
                        if (entry1Map == null) {
                            entry1Map = new HashMap<>();
                            result.put(entry1, entry1Map);
                        }
                        HashMap<String, Boolean> entry2Map = result.get(entry2);
                        if (entry2Map == null) {
                            entry2Map = new HashMap<>();
                            result.put(entry2, entry2Map);
                        }
                        if (line.contains("TRUE")) {
                            entry1Map.put(entry2, true);
                            entry2Map.put(entry1, true);
                            break;
                        } else if (line.contains("FALSE")) {
                            entry1Map.put(entry2, false);
                            entry2Map.put(entry1, false);
                            break;
                        }
                    }
                }
            }
        }

        //scriptFile.delete();
        //outputFile.delete();

        return result;
    }

}
