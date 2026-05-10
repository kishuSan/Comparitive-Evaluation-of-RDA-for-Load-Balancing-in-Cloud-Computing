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
import java.io.IOException;
import java.io.PrintWriter;

public class SimRDA {
    // no of runs
    private final static int runs = 30;

    //population
    private final static int population = 200;
    private final static int iterations = 100;

    // sensitivity parameters
    private final static double alpha = 0.5;
    private final static double beta = 0.2;
    private final static double gamma = 0.3;

    // fitness function weights for QOS parameters — must sum to 1.0
    private final static double w1 = 0.30; // makespan
    private final static double w2 = 0.25; // utilization
    private final static double w3 = 0.20; // cost
    private final static double w4 = 0.25; // response time

    public static void main(String[] args) {
        Log.setLevel(Level.WARN);
//        Log.setLevel(Level.OFF);
        for(int i = 0; i < runs; i++)
        {
            System.out.println("====> Simulation #" + i);
            runSimulation(i);
            if(i % 10 == 0) {
                System.gc();
            }
        }
    }

    private static void runSimulation(int iter)
    {
        CloudSimPlus simulation = new CloudSimPlus();

        Datacenter datacenter = SimulationUtils.createDatacenter(simulation);
        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);

        int numVMs = SimulationUtils.NUM_VMS;
        int numCloudlets = SimulationUtils.NUM_CLOUDLETS;

        List<Vm> vmList = SimulationUtils.createVMs(numVMs);
        List<Cloudlet> cloudletList = SimulationUtils.createCloudlets(numCloudlets);

        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);

        int[] vmAssignments = getVmAssignments(numVMs, numCloudlets);

        // bind cloudlets to VMs per RDA assignment
//        System.out.print("Best Solution: [");
        for (int i = 0; i < numCloudlets; i++) {
            broker.bindCloudletToVm(
                    cloudletList.get(i),
                    vmList.get(vmAssignments[i])
            );
//            System.out.print(vmAssignments[i] + (i < numCloudlets - 1 ? ", " : ""));
        }
//        System.out.println("]");

        simulation.start();
        List<Cloudlet> finished = broker.getCloudletFinishedList();

//        SimulationUtils.printCloudletResults(finished);

        System.out.println("Simulation Results:");

        int[] vmCount = new int[numVMs];
        for (int vmId : vmAssignments) vmCount[vmId]++;

        int max_requests_assigned = Arrays.stream(vmCount).max().getAsInt();
        int min = Arrays.stream(vmCount).min().getAsInt();
        double avg = Arrays.stream(vmCount).average().getAsDouble();
        int vmIDWithHighestAssignment = 0;
        for(int i = 0; i < numVMs; i++) if(max_requests_assigned == vmCount[i]) vmIDWithHighestAssignment = i;

        int success = SimulationUtils.getNumSuccessfulRuns(finished);
        double makespan = SimulationUtils.getMakeSpan(finished);
        double avgRT = SimulationUtils.getAvgRT(finished);
        double avgUtil = SimulationUtils.getAvgUtil(finished, vmList, makespan);
        double totalCost = SimulationUtils.getTotalCost(finished);
        double[] fitness_logs_for_convergence = AlgoRDA.getFitness_for_logs_convergence();
        double bestFitness = AlgoRDA.getBestFitness();

        System.out.println("SuccessfulRuns: " + success + "/" + 1000);
        System.out.println("VM load - max: " + max_requests_assigned + ", min: " + min + ", avg: " + avg);
        System.out.println("VM with highest assigned requests: " + vmIDWithHighestAssignment);
        System.out.println("BestFitness: " + bestFitness);
        System.out.println("Makespan: " + makespan);
        System.out.println("Average Response Time: " + avgRT);
        System.out.println("Average Utilization: " + avgUtil);
        System.out.println("Total Cost: " + totalCost);
        System.out.print("Fitness across runs: ");
        for(int i = 0; i < runs; i++) System.out.printf("%.6f ,", fitness_logs_for_convergence[i]);
        System.out.println();

        updateCSV(iter, success, finished.size(), makespan, avgRT, avgUtil, totalCost, bestFitness, max_requests_assigned);
        // cleanup
        vmList = null;
        cloudletList = null;
        finished = null;
        simulation = null;
        broker = null;
        datacenter = null;
    }

    private static int[] getVmAssignments(int numVMs, int numCloudlets) {
        double[] vmMips = SimulationUtils.getVmMips(numVMs);
        double[] vmCostRate = SimulationUtils.getVmCostRate(numVMs);
        double[] vmAvailability = SimulationUtils.getVmAvailability(numVMs);

        double[] cloudletLengths = SimulationUtils.createCloudletLengths(numCloudlets);

        // run RDA with full constructor
        AlgoRDA rda = new AlgoRDA(
                iterations,
                population,
                numVMs, numCloudlets,
                cloudletLengths, vmMips, vmCostRate, vmAvailability,
                w1, w2, w3, w4,
                alpha, beta, gamma
        );
        return rda.run();
    }


    // ============================================
    public static void updateCSV(
            int runId,
            int success,
            int totalCloudlets,
            double makespan,
            double avgRT,
            double avgUtil,
            double totalCost,
            double bestFitness,
            int max_requests_assigned
    ) {

        String fileName = "results.csv";

        try {

            File file = new File(fileName);

            boolean fileExists = file.exists();

            FileWriter fw = new FileWriter(file, true);
            PrintWriter pw = new PrintWriter(fw);


            if (!fileExists) {
                pw.println(
                        "Run," +
                        "Population," +
                        "Alpha," +
                        "Beta," +
                        "Gamma," +
                        "SuccessRate," +
                        "Makespan," +
                        "AvgResponseTime," +
                        "AvgUtilization," +
                        "TotalCost," +
                        "BestFitness," +
                        "Max Requests Assigned"
                );
            }

            double successRate = ((double) success / 1000) * 100.0;

            pw.println(
                    runId + "," +
                    population + "," +
                    String.format("%.2f", alpha) + "," +
                    String.format("%.2f", beta) + "," +
                    String.format("%.2f", gamma) + "," +
                    String.format("%.2f", successRate) + "," +
                    String.format("%.2f", makespan) + "," +
                    String.format("%.2f", avgRT) + "," +
                    String.format("%.2f", avgUtil) + "," +
                    String.format("%.2f", totalCost) + "," +
                    String.format("%.6f", bestFitness) + ',' +
                    String.format("%d", max_requests_assigned)
            );

            pw.close();

        } catch (IOException e) {
            System.out.println("CSV Write Error: " + e.getMessage());
        }
    }
}
