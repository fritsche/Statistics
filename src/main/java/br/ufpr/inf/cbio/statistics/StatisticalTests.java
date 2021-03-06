package br.ufpr.inf.cbio.statistics;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
//import org.apache.commons.math3.*;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

public class StatisticalTests {

    // < Indicator < Problem < Algorithm , values > > >
    private final HashMap<String, HashMap<String, HashMap<String, double[]>>> data;
    // < Indicator < Problem < Algorithm < Algorithm, isDiff > > > >
    private final HashMap<String, HashMap<String, HashMap<String, HashMap<String, Boolean>>>> statisticalData;

    private final HashMap<String, Boolean> isMinimization;

    public StatisticalTests() {
        isMinimization = new HashMap<>();
        isMinimization.put("HV", false);
        isMinimization.put("ARI", false);
        isMinimization.put("HVA", false);
        isMinimization.put("IGD", true);
        isMinimization.put("IGDP", true);
        isMinimization.put("R2", true);
        isMinimization.put("EP", true);
        isMinimization.put("GD", true);
        isMinimization.put("GDP", true);
        isMinimization.put("IGD+", true);

        data = new HashMap<>();
        statisticalData = new HashMap<>();

    }

    public void generateStatisticalTests(String indicator, List<String> problemNameList, List<String> algorithmNameList,
            String experimentBaseDirectory, int m, String experimentName, String group, float confidence) {
        HashMap<String, HashMap<String, HashMap<String, Boolean>>> output = new HashMap<>();
        HashMap<String, HashMap<String, double[]>> indicatormap = new HashMap<>();
        problemNameList.forEach((problem) -> {
            try {
                HashMap<String, double[]> values = new HashMap<>();

                algorithmNameList.forEach((algorithm) -> {
                    FileInputStream fis = null;
                    try {
                        ArrayList<Double> d = new ArrayList<>();
                        fis = new FileInputStream(
                                experimentBaseDirectory + "/" + m + "/" + group + "/" + algorithm + "/" + problem + "/" + indicator);
                        InputStreamReader isr = new InputStreamReader(fis);
                        BufferedReader br = new BufferedReader(isr);
                        String aux = br.readLine();
                        while (aux != null) {
                            d.add(Double.parseDouble(aux));
                            aux = br.readLine();
                        }
                        double[] dd = new double[d.size()];
                        for (int i = 0; i < d.size(); ++i) {
                            dd[i] = d.get(i);
                        }
                        values.put(algorithm, dd);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(StatisticalTests.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(StatisticalTests.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        try {
                            fis.close();
                        } catch (IOException ex) {
                            Logger.getLogger(StatisticalTests.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
                output.put(problem, KruskalWallisTest.test(values,
                        experimentBaseDirectory + "/R/" + experimentName + "/" + indicator + "/" + m, confidence));
                indicatormap.put(problem, values);
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(StatisticalTests.class.getName()).log(Level.SEVERE, null, ex);
            }

        });

        statisticalData.put(indicator, output);
        data.put(indicator, indicatormap);
    }

    public void generateLatexTables(List<String> indicatorNameList, List<String> problemNameList,
            List<String> algorithmNameList, String experimentBaseDirectory, String experimentName, int obj, boolean printSTD) {
        // < Indicator < Problem < Algorithm , value > > >
        HashMap<String, HashMap<String, HashMap<String, Double>>> mean = new HashMap<>();
        HashMap<String, HashMap<String, HashMap<String, Double>>> standardDeviation = new HashMap<>();
        HashMap<String, HashMap<String, HashMap<String, Boolean>>> grey = new HashMap<>();
        HashMap<String, HashMap<String, String>> bold = new HashMap<>();
        data.entrySet().forEach((indicator) -> {
            HashMap<String, HashMap<String, Double>> problemmean = new HashMap<>();
            HashMap<String, HashMap<String, Double>> problemstd = new HashMap<>();
            HashMap<String, HashMap<String, Boolean>> problemgrey = new HashMap<>();
            HashMap<String, String> problembold = new HashMap<>();
            indicator.getValue().entrySet().forEach((problem) -> {
                HashMap<String, Double> algorithmmean = new HashMap<>();
                HashMap<String, Double> algorithmstd = new HashMap<>();
                HashMap<String, Boolean> algorithmgrey = new HashMap<>();
                String sbest = "";
                boolean minimization = isMinimization.get(indicator.getKey());
                double best = (minimization) ? (Double.POSITIVE_INFINITY) : (Double.NEGATIVE_INFINITY);
                for (Map.Entry<String, double[]> algorithm : problem.getValue().entrySet()) {
                    Mean m = new Mean();
                    StandardDeviation std = new StandardDeviation();
                    for (double d : algorithm.getValue()) {
                        m.increment(d);
                        std.increment(d);
                    }
                    double value = m.getResult();
                    algorithmmean.put(algorithm.getKey(), value);
                    algorithmstd.put(algorithm.getKey(), std.getResult());
                    if ((value < best && minimization) || (value > best && !minimization)) {
                        sbest = algorithm.getKey();
                        best = value;
                    }
                }
                algorithmgrey.put(sbest, true); // gray to the best
                HashMap<String, Boolean> test = new HashMap<>();
                // TEST
                HashMap<String, HashMap<String, HashMap<String, Boolean>>> aux1 = statisticalData
                        .get(indicator.getKey());
                HashMap<String, HashMap<String, Boolean>> aux2 = aux1.get(problem.getKey());
                HashMap<String, Boolean> aux3 = aux2.get(sbest);
                java.util.Set aux4 = aux3.entrySet();

                for (Map.Entry<String, Boolean> b : statisticalData.get(indicator.getKey()).get(problem.getKey())
                        .get(sbest).entrySet()) {
                    algorithmgrey.put(b.getKey(), !b.getValue()); // if not different to the best then gray else not gray
                }
                problemmean.put(problem.getKey(), algorithmmean);
                problemstd.put(problem.getKey(), algorithmstd);
                problemgrey.put(problem.getKey(), algorithmgrey);
                problembold.put(problem.getKey(), sbest);
            });
            mean.put(indicator.getKey(), problemmean);
            standardDeviation.put(indicator.getKey(), problemstd);
            grey.put(indicator.getKey(), problemgrey);
            bold.put(indicator.getKey(), problembold);
        });

        FileWriter os, osbin;
        NumberFormat formatter = new DecimalFormat("0.00E0");

        for (String i : indicatorNameList) {
            try {
                checkDirectory(experimentBaseDirectory + "/R/" + experimentName + "/" + obj + "/");
                os = new FileWriter(experimentBaseDirectory + "/R/" + experimentName + "/" + obj + "/" + i + ".tex");
                osbin = new FileWriter(experimentBaseDirectory + "/R/" + experimentName + "/" + obj + "/" + i + ".bin");
                os.write("\n\\multirow{" + problemNameList.size() + "}{*}{" + obj + "}");
                for (String p : problemNameList) {
                    os.write(" & " + p);
                    for (String a : algorithmNameList) {
                        if (grey.get(i).get(p).get(a)) {
                            os.write(" & \\cellcolor{gray95}");
                        } else {
                            os.write(" &");
                        }

                        if (printSTD) {
                            if (bold.get(i).get(p).equals(a)) {
                                os.write("{\\bf " + formatter.format(mean.get(i).get(p).get(a)) + "("
                                        + formatter.format(standardDeviation.get(i).get(p).get(a)) + ")}");
                                osbin.write(" 1");
                            } else {
                                os.write("" + formatter.format(mean.get(i).get(p).get(a)) + "("
                                        + formatter.format(standardDeviation.get(i).get(p).get(a)) + ")");
                                osbin.write(" 0");
                            }
                        } else {
                            if (bold.get(i).get(p).equals(a)) {
                                os.write("{\\bf " + formatter.format(mean.get(i).get(p).get(a)) + "}");
                                osbin.write(" 1");
                            } else {
                                os.write("" + formatter.format(mean.get(i).get(p).get(a)));
                                osbin.write(" 0");
                            }
                        }
                    }
                    osbin.write("\n");
                    os.write("\\\\\n");
                }
                os.close();
                osbin.close();
            } catch (IOException ex) {
                Logger.getLogger(StatisticalTests.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void generateOverallStatisticalTest(String indicator, int[] mm, List<String> problemNameList,
            List<String> algorithmNameList, String experimentBaseDirectory, String experimentName, String group, float confidence, boolean printSTD) {
        try {
            HashMap<String, double[]> values = new HashMap<>();
            int i, j, k;
            int posicion;
            int ig;
            double sum;
            double Rj[];
            double std[];
            boolean encontrado;
            boolean visto[];
            List porVisitar;
            for (String algorithm : algorithmNameList) {
                // System.out.println(algorithm);
                ArrayList<Double> d = new ArrayList<>();
                for (int m : mm) {
                    for (String problem : problemNameList) {
                        FileInputStream fis = new FileInputStream(
                                experimentBaseDirectory + "/" + m + "/" + group + "/" + algorithm + "/" + problem + "/" + indicator);
                        try {
                            InputStreamReader isr = new InputStreamReader(fis);
                            BufferedReader br = new BufferedReader(isr);
                            String aux = br.readLine();
                            sum = 0.0;
                            int count = 0;
                            while (aux != null) {
                                sum += Double.parseDouble(aux);
                                count++;
                                aux = br.readLine();
                            }
                            d.add((sum / (double) count));
                            // System.out.println((sum/(double)count));
                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(StatisticalTests.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(StatisticalTests.class.getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            try {
                                fis.close();
                            } catch (IOException ex) {
                                Logger.getLogger(StatisticalTests.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
                // System.out.println();
                double[] dd = new double[d.size()];
                for (i = 0; i < d.size(); ++i) {
                    dd[i] = d.get(i);
                }
                values.put(algorithm, dd);
            }
            ArrayList<double[]> matrix = new ArrayList<>();
            int size = 0;
            for (Map.Entry<String, double[]> entrySet : values.entrySet()) {
                // System.out.println(entrySet.getKey());
                double[] keyValues = entrySet.getValue();
                matrix.add(keyValues);
                size = keyValues.length;
            }
            Pareja[][] orden = new Pareja[size][matrix.size()];
            for (i = 0; i < size; ++i) {
                for (j = 0; j < matrix.size(); ++j) {
                    orden[i][j] = new Pareja(j, matrix.get(j)[i]);
                }
                if (isMinimization.get(indicator)) {
                    Arrays.sort(orden[i]);
                } else {
                    Arrays.sort(orden[i], Collections.reverseOrder());
                }
            }
            /* building of the rankings table per algorithms and data sets */
            Pareja[][] rank = new Pareja[size][matrix.size()];
            posicion = 0;
            for (i = 0; i < size; i++) {
                for (j = 0; j < matrix.size(); j++) {
                    encontrado = false;
                    for (k = 0; k < matrix.size() && !encontrado; k++) {
                        if (orden[i][k].indice == j) {
                            encontrado = true;
                            posicion = k + 1;
                        }
                    }
                    rank[i][j] = new Pareja(posicion, orden[i][posicion - 1].valor);
                }
            }
            /*
            * In the case of having the same performance, the rankings are
            * equal
             */
            for (i = 0; i < size; i++) {
                visto = new boolean[matrix.size()];
                porVisitar = new ArrayList();

                Arrays.fill(visto, false);
                for (j = 0; j < matrix.size(); j++) {
                    porVisitar.clear();
                    sum = rank[i][j].indice;
                    visto[j] = true;
                    ig = 1;
                    for (k = j + 1; k < matrix.size(); k++) {
                        if (rank[i][j].valor == rank[i][k].valor && !visto[k]) {
                            sum += rank[i][k].indice;
                            ig++;
                            porVisitar.add(k);
                            visto[k] = true;
                        }
                    }
                    sum /= (double) ig;
                    rank[i][j].indice = sum;
                    for (k = 0; k < porVisitar.size(); k++) {
                        rank[i][((Integer) porVisitar.get(k))].indice = sum;
                    }
                }
            }
            /* compute the average ranking for each algorithm */
            String sbest = "";
            double best = Double.POSITIVE_INFINITY;
            Rj = new double[matrix.size()];
            std = new double[matrix.size()];
            i = 0;
            for (Map.Entry<String, double[]> entrySet : values.entrySet()) {
                // for (i=0; i<matrix.size(); i++){
                Rj[i] = 0;
                std[i] = 0;
                for (j = 0; j < size; j++) {
                    Rj[i] += rank[j][i].indice / ((double) size);
                }
                // standard deviation
                for (j = 0; j < size; j++) {
                    std[i] += (rank[j][i].indice - Rj[i]) * (rank[j][i].indice - Rj[i]);
                }
                std[i] /= ((double) size);
                std[i] = Math.sqrt(std[i]);

                if (Rj[i] < best) {
                    best = Rj[i];
                    sbest = entrySet.getKey();
                }
                i++;
            }
            String outDir = experimentBaseDirectory + "/R/" + experimentName + "";
            String outFile = outDir + "/FriedmanTest" + indicator + ".tex";
            if (!(new File(outDir)).exists()) {
                (new File(outDir)).mkdirs();
            }
            HashMap<String, HashMap<String, Boolean>> result = FriedmanTest.test(values, outDir + "/" + indicator + "", isMinimization.get(indicator), confidence);
            boolean difference = true;
            for (Map.Entry<String, Boolean> b : result.get(sbest).entrySet()) {
                if (!b.getValue()) { // if there is NOT statistical difference
                    difference = false;
                    break;
                }
            }
            NumberFormat formatter = new DecimalFormat("0.000");
            String Output = "";
            Output = Output + ("\\documentclass{article}\n" + "\\usepackage{graphicx}"
                    + "\\usepackage{colortbl}\n"
                    + "\\usepackage[table*]{xcolor}\n"
                    + "\\usepackage{multirow}\n"
                    + "\\usepackage{fixltx2e}\n"
                    + "\\usepackage{stfloats}\n"
                    + "\\usepackage{psfrag}\n"
                    + "\\usepackage[]{threeparttable}\n"
                    + "\\usepackage{multicol}\n"
                    + "\\usepackage{lscape}\n"
                    + "\\xdefinecolor{gray95}{gray}{0.75}"
                    + "\n" + "\\title{Results}\n"
                    + "\\author{}\n" + "\\date{\\today}\n" + "\\begin{document}\n"
                    + "\\oddsidemargin 0in \\topmargin 0in" + "\\maketitle\n" + "\\section{Tables of Friedman Tests}");
            /* Print the average ranking per algorithm */
            Output = Output + "\n"
                    + ("\\begin{table}[!htp]\n" + "\\centering\n" + "\\caption{Average Rankings of the algorithms\n}"
                    + // for
                    // "+
                    // exp_.problemList_[prob]
                    // +"
                    // problem\n}"
                    // +
                    "\\begin{tabular}{|c|c|}\n" + "\\hline\nAlgorithm&Ranking\\\\\n\\hline");
            i = 0;
            if (printSTD) {
                for (Map.Entry<String, double[]> entrySet : values.entrySet()) {
                    // for (i=0; i<matrix.size();i++) {
                    if (entrySet.getKey().equals(sbest) && difference) {
                        Output = Output + "\n" + (String) entrySet.getKey() + "& \\cellcolor{gray95} {\\bf "
                                + formatter.format(Rj[i]) + "(" + formatter.format(std[i]) + ")}\\\\\\hline";
                    } else if (entrySet.getKey().equals(sbest)) {
                        Output = Output + "\n" + (String) entrySet.getKey() + "& {\\bf " + formatter.format(Rj[i]) + "("
                                + formatter.format(std[i]) + ")}\\\\\\hline";
                    } else {
                        Output = Output + "\n" + (String) entrySet.getKey() + "&" + formatter.format(Rj[i]) + "("
                                + formatter.format(std[i]) + ")\\\\\\hline";
                    }
                    i++;
                }
            } else {
                for (Map.Entry<String, double[]> entrySet : values.entrySet()) {
                    // for (i=0; i<matrix.size();i++) {
                    if (entrySet.getKey().equals(sbest) && difference) {
                        Output = Output + "\n" + (String) entrySet.getKey() + "& \\cellcolor{gray95} {\\bf "
                                + formatter.format(Rj[i]) + "}\\\\\\hline";
                    } else if (entrySet.getKey().equals(sbest)) {
                        Output = Output + "\n" + (String) entrySet.getKey() + "& {\\bf " + formatter.format(Rj[i]) + "}\\\\\\hline";
                    } else {
                        Output = Output + "\n" + (String) entrySet.getKey() + "&" + formatter.format(Rj[i]) + "\\\\\\hline";
                    }
                    i++;
                }
            }
            Output = Output + "\n" + "\\end{tabular}\n" + "\\end{table}";
            Output = Output + "\n" + "\\end{document}";
            // try {
            File latexOutput;
            latexOutput = new File(outDir);
            if (!latexOutput.exists()) {
                latexOutput.mkdirs();
            }
            try (FileOutputStream f = new FileOutputStream(outFile); DataOutputStream fis = new DataOutputStream((OutputStream) f)) {
                fis.writeBytes(Output);
                // }
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(StatisticalTests.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void checkDirectory(String directory) {
        File directory_;

        directory_ = new File(directory);
        if (!directory_.exists()) {
            new File(directory).mkdirs();
        }
    }

    public static void main(String[] args) {

        // params:
        //
        // indicator name
        // number of algorithms
        // algorithm name list
        // number of problems
        // problem list
        // number of objective configs
        // objective list
        // output directory
        // experiment name
        // confidence level [eg.: 0.99]
        // print std [TRUE|FALSE]
        LinkedList<String> arguments = new LinkedList<>(Arrays.asList(args));

        Iterator<String> iterator = arguments.iterator();

        String indicator = iterator.next();
        System.out.println("Indicator: " + indicator);

        int numAlgs = Integer.parseInt(iterator.next());
        System.out.println("numAlgs: " + numAlgs);
        List<String> algorithmNameList = new ArrayList<>(numAlgs);
        for (int i = 0; i < numAlgs; i++) {
            algorithmNameList.add(iterator.next());
            System.out.println("\talgorithm: " + algorithmNameList.get(i));
        }

        List<String> problemNameList = new ArrayList<>();
        int numProbs = Integer.parseInt(iterator.next());
        System.out.println("numProbs: " + numProbs);
        for (int i = 0; i < numProbs; i++) {
            problemNameList.add(iterator.next());
            System.out.println("\tproblem: " + problemNameList.get(i));
        }

        int numObj = Integer.parseInt(iterator.next());
        System.out.println("numObj: " + numObj);
        int objectives[] = new int[numObj];
        for (int i = 0; i < numObj; i++) {
            objectives[i] = (Integer.parseInt(iterator.next()));
            System.out.println("\tobjectives: " + objectives[i]);
        }

        String outputDir = iterator.next();
        System.out.println("outputDir: " + outputDir);

        String experimentName = iterator.next();
        System.out.println("experimentName: " + experimentName);

        String group = iterator.next(); // subfolder for group of algorithms
        System.out.println("group: " + group);

        float confidence = Float.parseFloat(iterator.next());

        boolean printSTD = Boolean.parseBoolean(iterator.next());

        List<String> indicatorNameList = new ArrayList<>();
        indicatorNameList.add(indicator);
        StatisticalTests tests = new StatisticalTests();

        for (int m : objectives) {

            tests.generateStatisticalTests(indicator, problemNameList, algorithmNameList, outputDir, m,
                    experimentName, group, confidence);

            tests.generateLatexTables(indicatorNameList, problemNameList, algorithmNameList, outputDir,
                    experimentName, m, printSTD);
        }

        tests.generateOverallStatisticalTest(indicator, objectives, problemNameList, algorithmNameList,
                outputDir, experimentName, group, confidence, printSTD);
    }
}