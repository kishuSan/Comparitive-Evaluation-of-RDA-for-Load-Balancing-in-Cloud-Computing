package org.loadbalancersim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.utilizationmodels.UtilizationModel;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

public class SimulationUtils {

    // --- SCALING CONFIG ---
    public static final int NUM_HOSTS = 20;
    public static final int NUM_VMS = 50;
    public static final int NUM_CLOUDLETS = 1000;

    // Host configs
    public static final int HOST_HIGH_CORES = 16;
    public static final int HOST_MED_CORES  = 8;
    public static final int HOST_LOW_CORES  = 4;

    public static final int HOST_HIGH_MIPS = 4000;
    public static final int HOST_MED_MIPS  = 2500;
    public static final int HOST_LOW_MIPS  = 1500;

    public static final long HOST_HIGH_RAM = 65536;
    public static final long HOST_MED_RAM  = 32768;
    public static final long HOST_LOW_RAM  = 16384;

    // VM Types (reusable patterns)
    public static final int[] VM_MIPS_TYPES = {1000, 2000, 3000, 4000};
    public static final double[] VM_COST_TYPES = {0.05, 0.08, 0.10, 0.15};
    public static final double[] VM_AVAILABILITY_TYPES = {1.0, 0.9, 0.8, 0.7};

    private static final Random rand_42 = new Random(42);
    private static final Random rand_84 = new Random(84);


    public static Datacenter createDatacenter(CloudSimPlus simulation) {
        List<Host> hostList = new ArrayList<>();

        for (int i = 0; i < NUM_HOSTS; i++) {

            int cores, mips;
            long ram;

            if (i % 3 == 0) {
                cores = HOST_HIGH_CORES;
                mips  = HOST_HIGH_MIPS;
                ram   = HOST_HIGH_RAM;
            } else if (i % 3 == 1) {
                cores = HOST_MED_CORES;
                mips  = HOST_MED_MIPS;
                ram   = HOST_MED_RAM;
            } else {
                cores = HOST_LOW_CORES;
                mips  = HOST_LOW_MIPS;
                ram   = HOST_LOW_RAM;
            }

            hostList.add(createHost(cores, mips, ram));
        }

        return new DatacenterSimple(simulation, hostList);
    }

    public static double[] getVmMips(int numVMs) {
        double[] mips = new double[numVMs];
        for (int i = 0; i < numVMs; i++) {
            mips[i] = VM_MIPS_TYPES[i % VM_MIPS_TYPES.length];
        }
        return mips;
    }

    public static double[] getVmCostRate(int numVMs) {
        double[] cost = new double[numVMs];
        for (int i = 0; i < numVMs; i++) {
            cost[i] = VM_COST_TYPES[i % VM_COST_TYPES.length];
        }
        return cost;
    }

    public static double[] getVmAvailability(int numVMs) {
        double[] avail = new double[numVMs];
        for (int i = 0; i < numVMs; i++) {
            avail[i] = VM_AVAILABILITY_TYPES[i % VM_AVAILABILITY_TYPES.length];
        }
        return avail;
    }

    private static Host createHost(int cores, int mips, long ram) {
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < cores; i++) peList.add(new PeSimple(mips));

        Host host = new HostSimple(ram, 10000, 1000000, peList);
        host.setVmScheduler(new VmSchedulerTimeShared());

        return host;
    }

    public static List<Vm> createVMs(int numVMs) {
        List<Vm> vmList = new ArrayList<>();

        for (int i = 0; i < numVMs; i++) {
            int mips = VM_MIPS_TYPES[i % VM_MIPS_TYPES.length];
            Vm vm = new VmSimple(mips, 1);
//            vm.setCloudletScheduler(new CloudletSchedulerTimeShared());
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            vm.setRam(2048).setBw(1000).setSize(10000);
            vmList.add(vm);
        }
        return vmList;
    }

    public static double[] createCloudletLengths(int numCloudlets) {
        double[] lengths = new double[numCloudlets];

        for (int i = 0; i < numCloudlets; i++) {
            double p = rand_42.nextDouble();
            if (p < 0.6) lengths[i] = 5000 + rand_42.nextInt(10000);
            else if (p < 0.9) lengths[i] = 15000 + rand_42.nextInt(30000);
            else lengths[i] = 10000 + rand_42.nextInt(40000);
        }
        return lengths;
    }

    public static List<Cloudlet> createCloudlets(int numCloudlets) {
        List<Cloudlet> cloudletList = new ArrayList<>();
        double[] lengths = createCloudletLengths(numCloudlets);
        for (int i = 0; i < numCloudlets; i++) {
            Cloudlet cloudlet = new CloudletSimple((long) lengths[i], 1);
            cloudlet.setUtilizationModelCpu(new UtilizationModelFull());
            cloudlet.setUtilizationModelRam(UtilizationModel.NULL);
            cloudlet.setUtilizationModelBw(UtilizationModel.NULL);
            cloudletList.add(cloudlet);
        }
        return cloudletList;
    }

    // result calculation region
    public static void printCloudletResults(List<Cloudlet> list) {
        for (Cloudlet cl : list) {
            System.out.println(
                "Cloudlet ID: " + cl.getId() +
                    " | VM: " + (cl.getVm() != null ? cl.getVm().getId() : -1) +
                    " | Status: " + cl.getStatus()
            );
        }
    }

    public static int getNumSuccessfulRuns(List<Cloudlet> list)
    {
        int success = 0;
        for(Cloudlet cl : list) if(cl.getStatus() == Cloudlet.Status.SUCCESS) success++;
        return success;
    }

    public static double getMakeSpan(List<Cloudlet> list) {
        double makespan = 0;
        for (Cloudlet cl : list)
            if (cl.getStatus() == Cloudlet.Status.SUCCESS) makespan = Math.max(makespan, cl.getFinishTime());
        return makespan;
    }

    public static double getAvgRT(List<Cloudlet> list) {
        double total = 0;
        for (Cloudlet cl : list) total += cl.getFinishTime() - cl.getBrokerArrivalTime();
        return list.isEmpty() ? 0 : total / list.size();
    }

    public static double getTotalCost(List<Cloudlet> list) {
        double[] vmCostRate = getVmCostRate(NUM_VMS); // already available here
        double totalCost = 0;
        for (Cloudlet cl : list) {
            int vmIdx = (int)(cl.getVm().getId() % vmCostRate.length);
            totalCost += cl.getTotalExecutionTime() * vmCostRate[vmIdx];
        }
        return totalCost;
    }

    public static double getAvgUtil(List<Cloudlet> list, List<Vm> vmList, double makespan) {
        double totalUtil = 0;
        for (Vm vm : vmList) {
            double vmTime = 0;
            for (Cloudlet cl : list) {
                if (cl.getStatus() == Cloudlet.Status.SUCCESS && cl.getVm().getId() == vm.getId()) {
                    vmTime += cl.getTotalExecutionTime();
                }
            }
            double vmUtil = (makespan > 0) ? Math.min(vmTime / makespan, 1.0) : 0;
            totalUtil += vmUtil;
        }
        return vmList.isEmpty() ? 0 : totalUtil / vmList.size();
    }
}
