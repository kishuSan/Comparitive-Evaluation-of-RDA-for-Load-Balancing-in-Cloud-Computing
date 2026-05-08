package org.loadbalancersim;

import java.util.List;

class QoSMetrics {
    public final double makespan;
    public final double utilization;
    public final double cost;
    public final double avgResponseTime;

    public QoSMetrics(double makespan, double utilization, double cost, double avgResponseTime) {
        this.makespan = makespan;
        this.utilization = utilization;
        this.cost = cost;
        this.avgResponseTime = avgResponseTime;
    }
}

class PopulationBounds {
    public final double minMakespan, maxMakespan;
    public final double minUtil, maxUtil;
    public final double minCost, maxCost;
    public final double minRT, maxRT;

    public PopulationBounds(
        double minMakespan, double maxMakespan,
        double minUtil, double maxUtil,
        double minCost, double maxCost,
        double minRT, double maxRT
    ) {
        this.minMakespan = minMakespan; this.maxMakespan = maxMakespan;
        this.minUtil = minUtil; this.maxUtil = maxUtil;
        this.minCost = minCost; this.maxCost = maxCost;
        this.minRT = minRT; this.maxRT = maxRT;
    }
}

public class FitnessFunction {

    private final int numVMs;
    private final int numCloudlets;
    private final double[] cloudletLength;  // MI(Million instructions) per cloudlet
    private final double[] vmMips;          // processing speed per VM
    private final double[] vmCostRate;      // cost per second per VM
    private final double[] vmAvailability;  // baseline availability [0.0 - 1.0] (pre-existing load)

    private static final double MIN_AVAILABILITY = 0.1; // floor so VM never hits zero MIPS

    // weights (must sum to 1.0)
    private final double w1; // makespan      (minimize)
    private final double w2; // utilization   (maximize)
    private final double w3; // cost          (minimize)
    private final double w4; // response time (minimize)

    public FitnessFunction(
        int numVMs,
        int numCloudlets,
        double[] cloudletLength,
        double[] vmMips,
        double[] vmCostRate,
        double[] vmAvailability,
        double w1, double w2, double w3, double w4
    ) {
        if (Math.abs(w1 + w2 + w3 + w4 - 1.0) > 1e-6)
            throw new IllegalArgumentException("Weights must sum to 1.0");
        if (vmMips.length != numVMs || vmCostRate.length != numVMs || vmAvailability.length != numVMs)
            throw new IllegalArgumentException("VM arrays must all have length == numVMs");
        if (cloudletLength.length != numCloudlets)
            throw new IllegalArgumentException("cloudletLength array must have length == numCloudlets");

        this.numVMs = numVMs;
        this.numCloudlets = numCloudlets;
        this.cloudletLength = cloudletLength;
        this.vmMips = vmMips;
        this.vmCostRate = vmCostRate;
        this.vmAvailability = vmAvailability;
        this.w1 = w1;
        this.w2 = w2;
        this.w3 = w3;
        this.w4 = w4;
    }

    // ---------------------------------------------------------------
    // PASS 1 — compute raw QoS metrics for a single solution
    // ---------------------------------------------------------------
    public QoSMetrics evaluateRaw(double[] position) {

        // decode solution vector → VM assignment
        int[] assignment = new int[numCloudlets];
        for (int i = 0; i < numCloudlets; i++) {
            assignment[i] = Math.min((int) Math.floor(position[i]), numVMs - 1);
        }

        double[] vmProcessingTime = new double[numVMs];
        double[] responseTime = new double[numCloudlets];
        double[] vmCostTotal = new double[numVMs];

        for (int i = 0; i < numCloudlets; i++) {
            int vm = assignment[i];

            // --- dynamic availability ---
            // step 1: find the most loaded VM so far in this evaluation
            double currentMaxLoad = 0;
            for (double t : vmProcessingTime) currentMaxLoad = Math.max(currentMaxLoad, t);

            // step 2: derive how relatively loaded this VM is right now
            // first cloudlet on any VM → dynamicAvailability = 1.0
            // as VM accumulates more work relative to others → degrades toward MIN_AVAILABILITY
            double dynamicAvailability = (currentMaxLoad > 0) ? 1.0 - (vmProcessingTime[vm] / currentMaxLoad) : 1.0;
            dynamicAvailability = Math.max(dynamicAvailability, MIN_AVAILABILITY);

            // step 3: combine baseline (pre-existing load) with dynamic (assignment-driven load)
            // vmAvailability[vm] = what the VM had before this batch arrived
            // dynamicAvailability = how much headroom remains as we assign more to it now
            double effectiveMips = vmMips[vm] * vmAvailability[vm] * dynamicAvailability;

            // SpaceShared: cloudlet starts after all previous cloudlets on this VM finish
            double execTime = cloudletLength[i] / effectiveMips;

            vmProcessingTime[vm] += execTime;
            responseTime[i] = vmProcessingTime[vm]; // cumulative = RT under TimeSharedScheduler
            vmCostTotal[vm] += execTime * vmCostRate[vm];
        }

        // makespan — Equation 3
        double makespan = 0;
        for (double t : vmProcessingTime) makespan = Math.max(makespan, t);

        // resource utilization — Equations 4, 5
        double totalUtilization = 0;
        for (double t : vmProcessingTime) totalUtilization += (makespan > 0) ? (t / makespan) : 0;
        double utilization = totalUtilization / numVMs;

        // average response time — Equation 7
        double avgRT = 0;
        for (double rt : responseTime) avgRT += rt;
        avgRT /= numCloudlets;

        // total cost — Equation 8
        double totalCost = 0;
        for (double c : vmCostTotal) totalCost += c;

        return new QoSMetrics(makespan, utilization, totalCost, avgRT);
    }

    // ---------------------------------------------------------------
    // PASS 2 — normalize across population and compute final fitness
    // implements Equations 9, 10, 11
    // ---------------------------------------------------------------
    public double normalizeAndScore(QoSMetrics m, PopulationBounds bounds) {

        // Equation 9 — metrics we MINIMIZE (lower raw value → lower normalized score → better)
        double nMakespan = normalizeMin(m.makespan, bounds.minMakespan, bounds.maxMakespan);
        double nCost = normalizeMin(m.cost, bounds.minCost, bounds.maxCost);
        double nRT = normalizeMin(m.avgResponseTime, bounds.minRT, bounds.maxRT);

        // Equation 10 — metrics we MAXIMIZE (higher raw value → higher normalized score → better)
        double nUtil = normalizeMax(m.utilization, bounds.minUtil, bounds.maxUtil);

        // Equation 11 — weighted fitness
        // (1.0 - nUtil) inverts utilization so that all four terms are "lower = better"
        // meaning the overall fitness is minimized by the RDA like all other terms
        return w1 * nMakespan + w2 * (1.0 - nUtil) + w3 * nCost + w4 * nRT;
    }

    // ---------------------------------------------------------------
    // compute population-wide min/max bounds across all raw metrics
    // called once between pass 1 and pass 2 each iteration
    // ---------------------------------------------------------------
    public PopulationBounds computeBounds(List<QoSMetrics> allMetrics) {
        double minMakespan = Double.MAX_VALUE, maxMakespan = -Double.MAX_VALUE;
        double minUtil = Double.MAX_VALUE, maxUtil = -Double.MAX_VALUE;
        double minCost = Double.MAX_VALUE, maxCost = -Double.MAX_VALUE;
        double minRT = Double.MAX_VALUE, maxRT = -Double.MAX_VALUE;

        for (QoSMetrics m : allMetrics) {
            minMakespan = Math.min(minMakespan, m.makespan);
            maxMakespan = Math.max(maxMakespan, m.makespan);
            minUtil = Math.min(minUtil, m.utilization);
            maxUtil = Math.max(maxUtil, m.utilization);
            minCost = Math.min(minCost, m.cost);
            maxCost = Math.max(maxCost, m.cost);
            minRT = Math.min(minRT, m.avgResponseTime);
            maxRT = Math.max(maxRT, m.avgResponseTime);
        }

        return new PopulationBounds(
            minMakespan, maxMakespan,
            minUtil, maxUtil,
            minCost, maxCost,
            minRT, maxRT
        );
    }

    // Equation 9 — normalize a metric we MINIMIZE
    // best solution (lowest raw value) → 0.0
    // worst solution (highest raw value) → 1.0
    private double normalizeMin(double value, double min, double max) {
        if (max <= min) return 1.0;
        return Math.max(0, Math.min(1, (max - value) / (max - min)));
    }

    // Equation 10 — normalize a metric we MAXIMIZE
    // best solution (highest raw value) → 1.0
    // worst solution (lowest raw value) → 0.0
    private double normalizeMax(double value, double min, double max) {
        if (max == min) return 1.0;
        return Math.max(0, Math.min(1, (value - min)/(max - min)));
    }
}
