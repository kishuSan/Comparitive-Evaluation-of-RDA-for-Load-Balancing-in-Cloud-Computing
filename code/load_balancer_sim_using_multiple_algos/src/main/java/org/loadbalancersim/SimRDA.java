package org.loadbalancersim;

import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.util.Log;
import ch.qos.logback.classic.Level;

import java.util.Arrays;
import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

enum AlgorithmType {
    RDA,
    PSO,
    WOA,
    GA,
    GWO
}

public class SimRDA {
    static class SimulationResult {
        double makespan;
        double avgRT;
        double avgUtil;
        double totalCost;
        double bestFitness;
        int maxLoad;
        double[] fitnessHistory;  // NEW: Track fitness per iteration
        double executionTime;

        public SimulationResult(
                double makespan,
                double avgRT,
                double avgUtil,
                double totalCost,
                double bestFitness,
                int maxLoad,
                double[] fitnessHistory,
                double executionTime) {
            this.makespan = makespan;
            this.avgRT = avgRT;
            this.avgUtil = avgUtil;
            this.totalCost = totalCost;
            this.bestFitness = bestFitness;
            this.maxLoad = maxLoad;
            this.fitnessHistory = fitnessHistory;
            this.executionTime = executionTime;
        }
    }

    // Configuration
    private final static int runs = 30;
    private final static int population = 100;
    private final static int iterations = 100;

    private final static int defaultCloudlets = SimulationUtils.NUM_CLOUDLETS;

    // Sensitivity parameters
    private final static double alpha = 0.35;
    private final static double beta = 0.3;
    private final static double gamma = 0.45;

    // Fitness weights (must sum to 1.0)
    private final static double w1 = 0.30; // makespan
    private final static double w2 = 0.25; // utilization
    private final static double w3 = 0.20; // cost
    private final static double w4 = 0.25; // response time

    public static void main(String[] args) {
        Log.setLevel(Level.WARN);

        System.out.println("=" + "=".repeat(60));
        System.out.println("SENSITIVITY ANALYSIS WITH CONVERGENCE DATA EXPORT");
        System.out.println("=" + "=".repeat(60));
        System.out.println();

        AlgorithmType algoType =  AlgorithmType.RDA;

         // Run all sensitivity analyses
//        new File("data/convergence").mkdirs();
//        new File("data/sensitivity").mkdirs();

//        performSensitivityAnalysis("Population", new double[] { 25, 30, 40, 50, 75, 100, 125, 150 }, algoType);
//        performSensitivityAnalysis("Iterations", new double[] { 10, 20, 40, 60, 80, 100, 125, 150}, algoType);
//        performSensitivityAnalysis("Alpha", new double[] { 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.6 }, algoType);
//        performSensitivityAnalysis("Beta", new double[] { 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.6 }, algoType);
//        performSensitivityAnalysis("Gamma", new double[] { 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.6 }, algoType);
//        performSensitivityAnalysis("Cloudlets", new double[]{ 100, 250, 500, 750, 1000, 1250, 1500, 1750, 2000 }, algoType);

        // algo comparison
        new File("data/comparison").mkdirs();
        new File("data/comparison/convergence").mkdirs();
//
//        performAlgorithmComparison(AlgorithmType.RDA);
        performAlgorithmComparison(AlgorithmType.PSO);
        performAlgorithmComparison(AlgorithmType.GA);
        performAlgorithmComparison(AlgorithmType.GWO);
        performAlgorithmComparison(AlgorithmType.WOA);

        System.out.println();
        System.out.println("=" + "=".repeat(60));
        System.out.println("COMPLETE! Data exported to:");
        System.out.println("  - data/convergence/      (iteration-level fitness data)");
        System.out.println("  - data/sensitivity/      (sensitivity analysis results)");
        System.out.println("=" + "=".repeat(60));
    }

    public static void performSensitivityAnalysis(
            String parameterName,
            double[] parameterValues,
            AlgorithmType algoType) {

        String fileName = "data/sensitivity/" + parameterName + "_sensitivity.csv";

        System.out.println("Generating: " + fileName);

        try {
            PrintWriter pw = new PrintWriter(new FileWriter(fileName));

            // CSV Header
            pw.println("ParameterValue,Run,ExecutionTime,Makespan,AvgRT,AvgUtilization,TotalCost,BestFitness,MaxLoad");

            // Loop through each parameter value
            for (double value : parameterValues) {
                System.out.println();
                System.out.println("=" + "=".repeat(60));
                System.out.println(parameterName + " = " + value);
                System.out.println("=" + "=".repeat(60));

                double[] makespanArr = new double[runs];
                double[] rtArr = new double[runs];
                double[] utilArr = new double[runs];
                double[] finalFitnessArr = new double[runs];

                // Run multiple simulations for this parameter value
                for (int run = 0; run < runs; run++) {
                    int localPopulation = population;
                    int localIterations = iterations;
                    int localCloudlets = defaultCloudlets;
                    double localAlpha = alpha;
                    double localBeta = beta;
                    double localGamma = gamma;

                    System.out.println("Run " + run);

                    // Apply parameter variation
                    switch (parameterName.toLowerCase()) {
                        case "population":
                            localPopulation = (int) value;
                            break;
                        case "iterations":
                            localIterations = (int) value;
                            break;
                        case "alpha":
                            localAlpha = value;
                            break;
                        case "beta":
                            localBeta = value;
                            break;
                        case "gamma":
                            localGamma = value;
                            break;
                        case "cloudlets":
                            localCloudlets = (int)value;
                            break;
                    }

                    SimulationResult result = runSimulation(
                            localPopulation,
                            localIterations,
                            localAlpha,
                            localBeta,
                            localGamma,
                            localCloudlets,
                            algoType);

                    // Store results for statistics
                    makespanArr[run] = result.makespan;
                    rtArr[run] = result.avgRT;
                    utilArr[run] = result.avgUtil;
                    finalFitnessArr[run] = result.bestFitness;

                    // ===== NEW: Export convergence data =====
                    ConvergenceDataExporter.exportConvergenceData(
                            run,
                            value,
                            parameterName,
                            result.fitnessHistory,
                            "data/convergence");

                    // Write to sensitivity CSV
                    pw.println(
                            value + "," +
                            run + "," +
                            result.executionTime + "," +
                            result.makespan + "," +
                            result.avgRT + "," +
                            result.avgUtil + "," +
                            result.totalCost + "," +
                            result.bestFitness + "," +
                            result.maxLoad);

                    // Progress indicator
                    if ((run + 1) % 5 == 0) {
                        System.out.println("  Completed " + (run + 1) + "/" + runs + " runs");
                    }
                }

                // ===== NEW: Export statistical summaries =====
                ConvergenceDataExporter.exportFinalFitnessSummary(
                        value,
                        parameterName,
                        finalFitnessArr,
                        "data/convergence");

                ConvergenceDataExporter.exportConvergenceStatistics(
                        value,
                        parameterName,
                        finalFitnessArr,
                        "data/convergence");

                // Print statistics
                System.out.println();
                System.out.println("Statistics for " + parameterName + " = " + value + ":");
                System.out.println("  Makespan:    Mean=" + mean(makespanArr) +
                                   ", StdDev=" + std(makespanArr));
                System.out.println("  Response Time: Mean=" + mean(rtArr) +
                                   ", StdDev=" + std(rtArr));
                System.out.println("  Utilization: Mean=" + mean(utilArr) +
                                   ", StdDev=" + std(utilArr));
                System.out.println("  Final Fitness: Mean=" + mean(finalFitnessArr) +
                                   ", StdDev=" + std(finalFitnessArr));

                // Garbage collection
                if (value == parameterValues[parameterValues.length / 2]) {
                    System.gc();
                }
            }

            pw.close();
            System.out.println();
            System.out.println("✓ Saved: " + fileName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void performAlgorithmComparison(AlgorithmType algoType)
    {
        try {
            String fileName = "data/comparison/" + algoType + "_results.csv";
            new File("data/comparison").mkdirs();
            PrintWriter pw = new PrintWriter(new FileWriter(fileName));
            pw.println(
                    "Algorithm,Run,ExecutionTime,Makespan,AvgRT,"
                    + "AvgUtilization,"
                    + "TotalCost,"
                    + "BestFitness,"
                    + "MaxLoad"
            );

            double[] fitnessArr = new double[runs];
            for(int run = 0; run < runs; run++)
            {
                System.out.println(algoType + " Run " + run);
                SimulationResult result = runSimulation(
                                population,
                                iterations,
                                alpha,
                                beta,
                                gamma,
                                defaultCloudlets,
                                algoType);

                pw.println(
                        algoType + "," +
                        run + "," +
                        result.executionTime + "," +
                        result.makespan + "," +
                        result.avgRT + "," +
                        result.avgUtil + "," +
                        result.totalCost + "," +
                        result.bestFitness + "," +
                        result.maxLoad
                );

                fitnessArr[run] = result.bestFitness;

                // Export convergence
                ConvergenceDataExporter.exportConvergenceData(
                    run,
                    0,
                    algoType.toString(),
                    result.fitnessHistory,
                    "data/comparison/convergence"
                );
            }

            ConvergenceDataExporter.exportFinalFitnessSummary(
                    0,
                    algoType.toString(),
                    fitnessArr,
                    "data/comparison"
            );

            ConvergenceDataExporter.exportConvergenceStatistics(
                    0,
                    algoType.toString(),
                    fitnessArr,
                    "data/comparison"
            );
            pw.close();
            System.out.println("✓ Saved: " + fileName);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private static SimulationResult runSimulation(
            int localPopulation,
            int localIterations,
            double localAlpha,
            double localBeta,
            double localGamma,
            int localCloudlets,
            AlgorithmType algoType) {

        CloudSimPlus simulation = new CloudSimPlus();
        Datacenter datacenter = SimulationUtils.createDatacenter(simulation);
        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);

        int numVMs = SimulationUtils.NUM_VMS;
//        int numCloudlets = SimulationUtils.NUM_CLOUDLETS;
        int numCloudlets = localCloudlets;

        List<Vm> vmList = SimulationUtils.createVMs(numVMs);
        List<Cloudlet> cloudletList = SimulationUtils.createCloudlets(numCloudlets);

        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);

        // Get VM and cloudlet parameters
        double[] vmMips = SimulationUtils.getVmMips(numVMs);
        double[] vmCostRate = SimulationUtils.getVmCostRate(numVMs);
        double[] vmAvailability = SimulationUtils.getVmAvailability(numVMs);
        double[] cloudletLengths = SimulationUtils.createCloudletLengths(numCloudlets);

        int[] vmAssignments = null;
        double[] fitnessHistory = null;
        double bestFitness = 0;

        // starting time
        long startTime = System.nanoTime();

        switch(algoType)
        {
            case RDA:
                AlgoRDA rda = new AlgoRDA(
                        localIterations,
                        localPopulation,
                        numVMs,
                        numCloudlets,
                        cloudletLengths,
                        vmMips,
                        vmCostRate,
                        vmAvailability,
                        w1,w2,w3,w4,
                        localAlpha,
                        localBeta,
                        localGamma
                );
                vmAssignments = rda.run();
                fitnessHistory = AlgoRDA.getFitness_for_logs_convergence();
                bestFitness = AlgoRDA.getBestFitness();
                break;

            case PSO:
                AlgoPSO pso = new AlgoPSO(
                        localIterations,
                        localPopulation,
                        numVMs,
                        numCloudlets,
                        cloudletLengths,
                        vmMips,
                        vmCostRate,
                        vmAvailability,
                        w1,w2,w3,w4
                );
                vmAssignments = pso.run();
                fitnessHistory = AlgoPSO.getFitness_for_logs_convergence();
                bestFitness = AlgoPSO.getBestFitness();
                break;

            case WOA:
                AlgoWOA woa = new AlgoWOA(
                        iterations,
                        population,
                        numVMs,
                        numCloudlets,
                        cloudletLengths,
                        vmMips,
                        vmCostRate,
                        vmAvailability,
                        w1,w2,w3,w4
                );

                vmAssignments = woa.run();

                fitnessHistory = AlgoWOA.getFitness_for_logs_convergence();
                bestFitness = AlgoWOA.getBestFitness();
                break;

            case GA:
                AlgoGA ga = new AlgoGA(
                        localIterations,
                        localPopulation,
                        numVMs,
                        numCloudlets,
                        cloudletLengths,
                        vmMips,
                        vmCostRate,
                        vmAvailability,
                        w1,w2,w3,w4
                );
                vmAssignments = ga.run();
                fitnessHistory = AlgoGA.getFitness_for_logs_convergence();
                bestFitness = AlgoGA.getBestFitness();
                break;

            case GWO:
                AlgoGWO gwo = new AlgoGWO(
                        localIterations,
                        localPopulation,
                        numVMs,
                        numCloudlets,
                        cloudletLengths,
                        vmMips,
                        vmCostRate,
                        vmAvailability,
                        w1,w2,w3,w4
                );
                vmAssignments = gwo.run();
                fitnessHistory = AlgoGWO.getFitness_for_logs_convergence();
                bestFitness = AlgoGWO.getBestFitness();
                break;
        }

        // finished execution time
        long endTime = System.nanoTime();
        double executionTime = (endTime - startTime) / 1_000_000.0;

        // Bind cloudlets to VMs
        for (int i = 0; i < numCloudlets; i++) {
            broker.bindCloudletToVm(cloudletList.get(i), vmList.get(vmAssignments[i]));
        }

        simulation.start();

        List<Cloudlet> finished = broker.getCloudletFinishedList();

        // Calculate metrics
        int[] vmCount = new int[numVMs];
        for (int vmId : vmAssignments) vmCount[vmId]++;
        int maxLoad = Arrays.stream(vmCount).max().getAsInt();

        double makespan = SimulationUtils.getMakeSpan(finished);
        double avgRT = SimulationUtils.getAvgRT(finished);
        double avgUtil = SimulationUtils.getAvgUtil(finished, vmList, makespan);
        double totalCost = SimulationUtils.getTotalCost(finished);

        return new SimulationResult(
                makespan, avgRT, avgUtil, totalCost,
                bestFitness, maxLoad, fitnessHistory,
                executionTime);
    }

    public static double mean(double[] arr) {
        double sum = 0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }

    public static double std(double[] arr) {
        double mean = mean(arr);
        double sum = 0;
        for (double v : arr) {
            sum += Math.pow(v - mean, 2);
        }
        return Math.sqrt(sum / arr.length);
    }
}