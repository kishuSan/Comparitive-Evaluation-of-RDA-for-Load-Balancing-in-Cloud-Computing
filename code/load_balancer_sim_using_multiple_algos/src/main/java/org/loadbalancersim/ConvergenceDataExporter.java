package org.loadbalancersim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Data Exporter for Convergence Analysis
 *
 * This class exports iteration-level fitness data from AlgoRDA runs
 * to CSV files that can be analyzed with statistical plots.
 *
 * Addresses Review Comments:
 * - Comment 6: Convergence Analysis
 * - Comment 3: Statistical Validation
 */
public class ConvergenceDataExporter {

    /**
     * Export convergence data for a single run.
     *
     * This should be called AFTER each AlgoRDA.run() completes.
     *
     * @param runId          The run number (0-29 for 30 runs)
     * @param paramValue     Value of the parameter being tested (e.g., population=100)
     * @param paramName      Name of parameter (e.g., "Population", "Alpha")
     * @param fitnessHistory Array of fitness values per iteration from AlgoRDA
     * @param outputDir      Directory to save CSV files
     */
    public static void exportConvergenceData(
            int runId,
            double paramValue,
            String paramName,
            double[] fitnessHistory,
            String outputDir) {

        String fileName = String.format("%s/convergence_%s_%.2f.csv",
                outputDir, paramName, paramValue);

        try {
            File file = new File(fileName);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            boolean fileExists = file.exists();
            FileWriter fw = new FileWriter(file, true);  // Append mode
            PrintWriter pw = new PrintWriter(fw);

            // Write header only if file is new
            if (!fileExists) {
                pw.println("Iteration,Run,Fitness");
            }

            // Write each iteration's fitness for this run
            for (int iteration = 0; iteration < fitnessHistory.length; iteration++) {
                pw.println(String.format("%d,%d,%.10f",
                        iteration, runId, fitnessHistory[iteration]));
            }

            pw.close();

        } catch (IOException e) {
            System.err.println("Error writing convergence data: " + e.getMessage());
        }
    }

    /**
     * Export final fitness summary across all runs for a parameter value.
     *
     * @param paramValue     Value of the parameter being tested
     * @param paramName      Name of parameter
     * @param allFinalFitness Array of final fitness values from all runs
     * @param outputDir      Directory to save CSV files
     */
    public static void exportFinalFitnessSummary(
            double paramValue,
            String paramName,
            double[] allFinalFitness,
            String outputDir) {

        String fileName = String.format("%s/final_fitness_%s_%.2f.csv",
                outputDir, paramName, paramValue);

        try {
            File file = new File(fileName);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            FileWriter fw = new FileWriter(file);
            PrintWriter pw = new PrintWriter(fw);

            pw.println("Run,FinalFitness");

            for (int run = 0; run < allFinalFitness.length; run++) {
                pw.println(String.format("%d,%.10f", run, allFinalFitness[run]));
            }

            pw.close();

        } catch (IOException e) {
            System.err.println("Error writing final fitness summary: " + e.getMessage());
        }
    }

    /**
     * Calculate and export convergence statistics.
     *
     * @param paramValue     Value of the parameter being tested
     * @param paramName      Name of parameter
     * @param allFinalFitness Array of final fitness values
     * @param outputDir      Directory to save CSV files
     */
    public static void exportConvergenceStatistics(
            double paramValue,
            String paramName,
            double[] allFinalFitness,
            String outputDir) {

        // Calculate statistics
        double mean = calculateMean(allFinalFitness);
        double stdDev = calculateStdDev(allFinalFitness, mean);
        double[] ci = calculate95ConfidenceInterval(allFinalFitness, mean, stdDev);
        double min = getMin(allFinalFitness);
        double max = getMax(allFinalFitness);
        double median = calculateMedian(allFinalFitness);

        String fileName = String.format("%s/statistics_%s.csv", outputDir, paramName);

        try {
            File file = new File(fileName);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            boolean fileExists = file.exists();
            FileWriter fw = new FileWriter(file, true);  // Append mode
            PrintWriter pw = new PrintWriter(fw);

            if (!fileExists) {
                pw.println("ParameterValue,Mean,StdDev,CI_Lower,CI_Upper,Min,Max,Median,NumRuns");
            }

            pw.println(String.format("%.2f,%.10f,%.10f,%.10f,%.10f,%.10f,%.10f,%.10f,%d",
                    paramValue, mean, stdDev, ci[0], ci[1], min, max, median,
                    allFinalFitness.length));

            pw.close();

        } catch (IOException e) {
            System.err.println("Error writing statistics: " + e.getMessage());
        }
    }

    // ========== Statistical Helper Functions ==========

    private static double calculateMean(double[] data) {
        double sum = 0;
        for (double value : data) {
            sum += value;
        }
        return sum / data.length;
    }

    private static double calculateStdDev(double[] data, double mean) {
        double sumSquaredDiff = 0;
        for (double value : data) {
            sumSquaredDiff += Math.pow(value - mean, 2);
        }
        return Math.sqrt(sumSquaredDiff / (data.length - 1));  // Sample std dev
    }

    private static double[] calculate95ConfidenceInterval(double[] data, double mean, double stdDev) {
        int n = data.length;
        double stdErr = stdDev / Math.sqrt(n);

        // t-distribution critical value for 95% CI (approximation for n > 30)
        double tValue = 1.96;  // For large n, use 1.96

        // For smaller samples, use more accurate t-values
        if (n <= 30) {
            // Approximate t-values for common sample sizes
            if (n <= 5) tValue = 2.776;
            else if (n <= 10) tValue = 2.262;
            else if (n <= 20) tValue = 2.093;
            else if (n <= 30) tValue = 2.045;
        }

        double marginError = tValue * stdErr;
        return new double[] { mean - marginError, mean + marginError };
    }

    private static double getMin(double[] data) {
        double min = Double.MAX_VALUE;
        for (double value : data) {
            if (value < min) min = value;
        }
        return min;
    }

    private static double getMax(double[] data) {
        double max = -Double.MAX_VALUE;
        for (double value : data) {
            if (value > max) max = value;
        }
        return max;
    }

    private static double calculateMedian(double[] data) {
        double[] sorted = data.clone();
        java.util.Arrays.sort(sorted);
        int n = sorted.length;
        if (n % 2 == 0) {
            return (sorted[n/2 - 1] + sorted[n/2]) / 2.0;
        } else {
            return sorted[n/2];
        }
    }
}