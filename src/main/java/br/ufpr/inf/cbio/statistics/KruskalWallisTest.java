package br.ufpr.inf.cbio.statistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class KruskalWallisTest {

    public static HashMap<String, HashMap<String, Boolean>> test(HashMap<String, double[]> values, String outputDir, float confidence) throws IOException, InterruptedException {

        float alpha = 1f - confidence;

        String script = "require(PMCMRplus)\noptions(\"width\"=10000)\n";
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
        script += "),each=" + size + "));";
        script += "\n";
        script += "result <- kruskal.test(ARRAY,categs)\n";
        script += "print(result);";
        script += "pos_teste<-kwAllPairsNemenyiTest(ARRAY, categs, method='Tukey');";
        script += "print(pos_teste);";

        StatisticalTests.checkDirectory(outputDir);
        File scriptFile = new File(outputDir + "/kruskalscript.R");
        File outputFile = new File(outputDir + "/kruskaloutput.R");

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

        HashMap<String, HashMap<String, Boolean>> result = new HashMap<>();
        HashMap<String, HashMap<String, Double>> matrix = new HashMap<>();

        int combinacoes = values.size();
        ArrayList<String> lines = new ArrayList<String>();
        Scanner scanner = new Scanner(outputFile);
        while (scanner.hasNextLine()) {
            lines.add(scanner.nextLine());
        }

        for (int i = lines.size() - combinacoes + 1; i < lines.size(); i++) {
            for (int j = 0; j < combinacoes - 1; j++) {
                String l = lines.get(i).split("\\s+")[0];
                String c = lines.get(lines.size() - combinacoes).split("\\s+")[j + 1];
                matrix.put(l, new HashMap<>());
                matrix.put(c, new HashMap<>());
            }
        }

        for (int i = lines.size() - combinacoes + 1; i < lines.size(); i++) {
            for (int j = 0; j < combinacoes - 1; j++) {
                String part = lines.get(i).replace("<", "").split("\\s+")[j + 1];
                if (part.compareTo("-") != 0) {
                    String l = lines.get(i).split("\\s+")[0];
                    String c = lines.get(lines.size() - combinacoes).split("\\s+")[j + 1];
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

                if (dvalue < alpha) {
                    result.get(key).put(key2, true);
                } else {
                    result.get(key).put(key2, false);
                }
            }
        }

        return result;
    }
}
