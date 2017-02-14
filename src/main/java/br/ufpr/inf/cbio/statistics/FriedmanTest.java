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

public class FriedmanTest {

    public static double confidence = 0.99;

    /**
     *
     * @param values
     * @param outputDir
     * @return matrix alg x alg a cell is true if there is statistical
     * difference between the alg_column and the alg_line false otherwise
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     *
     */
    public static HashMap<String, HashMap<String, Boolean>> test(HashMap<String, double[]> values, String outputDir, boolean isMinimization)
            throws IOException, InterruptedException {

        /**
         * To install graph and Rgraphviz: #
         * source("http://bioconductor.org/biocLite.R") #
         * biocLite(c("graph","Rgraphviz"))
         */
        String script = "require(\"PMCMR\")\n"
                + "options(\"width\"=10000)\n";
        String data = "ARRAY <- c(";
        int size = 0;
        for (Map.Entry<String, double[]> entrySet : values.entrySet()) {
            double[] keyValues = entrySet.getValue();
            size = keyValues.length;

            for (Double value : keyValues) {
                data += value + ",";
            }
        }
        data = data.substring(0, data.lastIndexOf(",")) + ")";
        data += "\n";

        data += "categs<-as.factor(rep(c(";
        for (Map.Entry<String, double[]> entrySet : values.entrySet()) {
            String key = entrySet.getKey();
            data += "\"" + key + "\",";
        }
        data = data.substring(0, data.lastIndexOf(","));
        data += "),each=" + size + "))";
        data += "\n";

        data += "probs<-as.factor(rep(c(";
        for (int i = 0; i < size; ++i) {
            data += "\"" + i + "\",";
        }
        data = data.substring(0, data.lastIndexOf(","));
        data += ")," + values.size() + "))";
        data += "\n";

        script += data;
        script += "result <- friedman.test(ARRAY,categs,probs)\n";
        script += "print(result);\n";
        script += "pos_teste<-posthoc.friedman.nemenyi.test(ARRAY, categs, probs, method='Tukey');\n";
        script += "print(pos_teste)\n";

        String scriptPlot = "require(\"scmamp\")\n";
        scriptPlot += data;
        if (isMinimization) { // if indicator is minimization (eg. IGD)
            scriptPlot += "neg = ARRAY * -1\n";
        } else { // if the indicator is maximization (eg. HV)
            scriptPlot += "neg = ARRAY\n";
        }

        scriptPlot += "d <- split(neg, ceiling(seq_along(neg)/" + size + "))\n"
                + "df <- data.frame(d)\n"
                + "colnames(df) <- unique(categs)\n"
                + "\n"
                + "setEPS()\n"
                + "postscript(\"" + outputDir + "/criticaldifference.eps\")\n"
                + "output <- plotCD(df, alpha=0.05 )\n"
                + "dev.off()\n";

        StatisticalTests.checkDirectory(outputDir);
        File scriptFile = new File(outputDir + "/friedmanscript.R");
        File scriptPlotFile = new File(outputDir + "/friedmanplot.R");
        File outputFile = new File(outputDir + "/friedmanoutput.R");

        try (FileWriter scriptWriter = new FileWriter(scriptFile)) {
            scriptWriter.append(script);
        }        
        
        try (FileWriter scriptWriter = new FileWriter(scriptPlotFile)) {
            scriptWriter.append(scriptPlot);
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
            throw new InterruptedException("R process failed! Check if R and depencencies are installed");
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
            for (int j = 0; j < combinacoes - 1; j++) {
                String l = lines.get(i).split("\\s+")[0];
                String c = lines.get(lines.size() - combinacoes - 2).split("\\s+")[j + 1];
                matrix.put(l, new HashMap<>());
                matrix.put(c, new HashMap<>());
            }
        }

        for (int i = lines.size() - combinacoes - 1; i < lines.size() - 2; i++) {
            double[] splittedValue = new double[combinacoes];

            for (int j = 0; j < combinacoes - 1; j++) {

                String part = lines.get(i).replace("<", "").split("\\s+")[j + 1];

                if (part.compareTo("-") != 0) {

                    String l = lines.get(i).split("\\s+")[0];
                    String c = lines.get(lines.size() - combinacoes - 2).split("\\s+")[j + 1];
                    matrix.get(l).put(c, Double.parseDouble(part));
                    matrix.get(c).put(l, Double.parseDouble(part));
                }
            }
        }

        for (Map.Entry<String, HashMap<String, Double>> entry : matrix.entrySet()) {
            String key = entry.getKey();
            HashMap<String, Double> value = entry.getValue();
            result.put(key, new HashMap<String, Boolean>());
            for (Map.Entry<String, Double> entry2 : value.entrySet()) {
                String key2 = entry2.getKey();
                double dvalue = entry2.getValue();

                if (dvalue < 0.05) {
                    result.get(key).put(key2, true);
                } else {
                    result.get(key).put(key2, false);
                }
            }
        }

        return result;

    }

}
