package org.loadbalancersim;

import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.vms.Vm;

import java.util.List;

public class SimRDA {

    public static void main(String[] args) {

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
        System.out.print("Best Solution: [");
        for (int i = 0; i < numCloudlets; i++) {
            broker.bindCloudletToVm(
                cloudletList.get(i),
                vmList.get(vmAssignments[i])
            );
            System.out.print(vmAssignments[i] + (i < numCloudlets - 1 ? ", " : ""));
        }
        System.out.println("]");

        simulation.start();
        List<Cloudlet> finished = broker.getCloudletFinishedList();

        SimulationUtils.printCloudletResults(finished);

        System.out.println("\nSimulation Results:");

        double makespan = SimulationUtils.getMakeSpan(finished);
        double avgRT = SimulationUtils.getAvgRT(finished);
        double avgUtil = SimulationUtils.getAvgUtil(finished, vmList, makespan);
        double totalCost = SimulationUtils.getTotalCost(finished, numVMs);

        System.out.println("Makespan: " + makespan);
        System.out.println("Average Response Time: " + avgRT);
        System.out.println("Average Utilization: " + avgUtil);
        System.out.println("Total Cost: " + totalCost);
    }

    private static int[] getVmAssignments(int numVMs, int numCloudlets) {
        double[] vmMips = SimulationUtils.getVmMips(numVMs);
        double[] vmCostRate = SimulationUtils.getVmCostRate(numVMs);
        double[] vmAvailability = SimulationUtils.getVmAvailability(numVMs);

        double[] cloudletLengths = SimulationUtils.createCloudletLengths(numCloudlets);

        // weights — must sum to 1.0
        double w1 = 0.35; // makespan
        double w2 = 0.25; // utilization
        double w3 = 0.20; // cost
        double w4 = 0.20; // response time

        // run RDA with full constructor
        AlgoRDA rda = new AlgoRDA(
            numVMs, numCloudlets,
            cloudletLengths, vmMips, vmCostRate, vmAvailability,
            w1, w2, w3, w4
        );
        return rda.run();
    }
}
