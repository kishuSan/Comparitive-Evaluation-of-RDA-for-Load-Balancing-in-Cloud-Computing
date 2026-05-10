package org.loadbalancersim;

import java.util.Arrays;
import java.util.Random;

/**
 * Whale Optimization Algorithm (WOA)
 * for Cloudlet-to-VM Scheduling
 *
 * Uses SAME fitness function structure as RDA/PSO
 * for fair comparison.
 */
public class AlgoWOA {

    private final int iterations;
    private final int populationSize;

    private final int numVMs;
    private final int numCloudlets;

    private final double[] cloudletLengths;
    private final double[] vmMips;
    private final double[] vmCostRate;
    private final double[] vmAvailability;

    // Fitness weights
    private final double w1;
    private final double w2;
    private final double w3;
    private final double w4;

    // Whale positions
    private double[][] whales;

    // Global best
    private double[] bestWhale;
    private double bestFitness =
            Double.MAX_VALUE;

    // Convergence logs
    private static double[] fitness_for_logs_convergence;

    private static double globalBestFitness;

    private final Random random = new Random();

    public AlgoWOA(
            int iterations,
            int populationSize,
            int numVMs,
            int numCloudlets,
            double[] cloudletLengths,
            double[] vmMips,
            double[] vmCostRate,
            double[] vmAvailability,
            double w1,
            double w2,
            double w3,
            double w4
    ) {

        this.iterations = iterations;
        this.populationSize = populationSize;

        this.numVMs = numVMs;
        this.numCloudlets = numCloudlets;

        this.cloudletLengths = cloudletLengths;
        this.vmMips = vmMips;
        this.vmCostRate = vmCostRate;
        this.vmAvailability = vmAvailability;

        this.w1 = w1;
        this.w2 = w2;
        this.w3 = w3;
        this.w4 = w4;

        initializeWhales();
    }

    // =========================================================
    // INITIALIZATION
    // =========================================================

    private void initializeWhales()
    {
        whales =
                new double[populationSize][numCloudlets];

        bestWhale =
                new double[numCloudlets];

        fitness_for_logs_convergence =
                new double[iterations];

        for(int i = 0; i < populationSize; i++)
        {
            for(int j = 0; j < numCloudlets; j++)
            {
                whales[i][j] =
                        random.nextInt(numVMs);
            }

            double fitness =
                    calculateFitness(
                            convertToIntArray(
                                    whales[i]
                            )
                    );

            if(fitness < bestFitness)
            {
                bestFitness = fitness;

                bestWhale =
                        Arrays.copyOf(
                                whales[i],
                                numCloudlets
                        );
            }
        }

        globalBestFitness = bestFitness;
    }

    // =========================================================
    // MAIN WOA LOOP
    // =========================================================

    public int[] run()
    {
        for(int it = 0; it < iterations; it++)
        {
            // Linearly decreasing coefficient
            double a =
                    2.0
                            - (2.0 * it / iterations);

            for(int i = 0; i < populationSize; i++)
            {
                double r1 = random.nextDouble();
                double r2 = random.nextDouble();

                double A =
                        2 * a * r1 - a;

                double C =
                        2 * r2;

                double p =
                        random.nextDouble();

                double l =
                        random.nextDouble() * 2 - 1;

                // =====================================================
                // UPDATE EACH DIMENSION
                // =====================================================

                for(int j = 0; j < numCloudlets; j++)
                {
                    if(p < 0.5)
                    {
                        // Exploitation
                        if(Math.abs(A) < 1)
                        {
                            double D =
                                    Math.abs(
                                            C * bestWhale[j]
                                                    - whales[i][j]
                                    );

                            whales[i][j] =
                                    bestWhale[j]
                                            - A * D;
                        }

                        // Exploration
                        else
                        {
                            int randIndex =
                                    random.nextInt(
                                            populationSize
                                    );

                            double[] randomWhale =
                                    whales[randIndex];

                            double D =
                                    Math.abs(
                                            C * randomWhale[j]
                                                    - whales[i][j]
                                    );

                            whales[i][j] =
                                    randomWhale[j]
                                            - A * D;
                        }
                    }

                    // Spiral update
                    else
                    {
                        double D =
                                Math.abs(
                                        bestWhale[j]
                                                - whales[i][j]
                                );

                        whales[i][j] =
                                D
                                        * Math.exp(1 * l)
                                        * Math.cos(
                                        2 * Math.PI * l
                                )
                                        + bestWhale[j];
                    }

                    // Clamp
                    if(whales[i][j] < 0)
                    {
                        whales[i][j] = 0;
                    }

                    if(whales[i][j] >= numVMs)
                    {
                        whales[i][j] =
                                numVMs - 1;
                    }
                }

                // =====================================================
                // FITNESS EVALUATION
                // =====================================================

                int[] solution =
                        convertToIntArray(
                                whales[i]
                        );

                double fitness =
                        calculateFitness(solution);

                // Update global best
                if(fitness < bestFitness)
                {
                    bestFitness = fitness;

                    bestWhale =
                            Arrays.copyOf(
                                    whales[i],
                                    numCloudlets
                            );
                }
            }

            fitness_for_logs_convergence[it] =
                    bestFitness;

            System.out.println(
                    "WOA Iteration "
                            + it
                            + " Best Fitness: "
                            + bestFitness
            );
        }

        globalBestFitness = bestFitness;

        return convertToIntArray(bestWhale);
    }

    // =========================================================
    // FITNESS FUNCTION
    // =========================================================

    private double calculateFitness(
            int[] solution
    )
    {
        double[] vmExecutionTime =
                new double[numVMs];

        double totalCost = 0;

        int[] vmLoad =
                new int[numVMs];

        for(int i = 0; i < numCloudlets; i++)
        {
            int vm = solution[i];

            double execTime =
                    cloudletLengths[i]
                            / vmMips[vm];

            vmExecutionTime[vm] += execTime;

            totalCost +=
                    execTime
                            * vmCostRate[vm];

            vmLoad[vm]++;
        }

        // Makespan
        double makespan =
                Arrays.stream(vmExecutionTime)
                        .max()
                        .getAsDouble();

        // Avg Response Time
        double avgRT =
                Arrays.stream(vmExecutionTime)
                        .average()
                        .orElse(0);

        // Utilization
        double totalBusy =
                Arrays.stream(vmExecutionTime)
                        .sum();

        double utilization =
                totalBusy
                        / (numVMs * makespan);

        // Load imbalance
        double imbalance =
                std(vmLoad);

        // Normalization
        double normMakespan =
                normalizeMin(
                        makespan,
                        0,
                        10000
                );

        double normRT =
                normalizeMin(
                        avgRT,
                        0,
                        10000
                );

        double normCost =
                normalizeMin(
                        totalCost,
                        0,
                        10000
                );

        double normUtil =
                normalizeMax(
                        utilization,
                        0,
                        1
                );

        double normImbalance =
                normalizeMin(
                        imbalance,
                        0,
                        100
                );

        // Final fitness
        return
                w1 * normMakespan
                        + w2 * (1 - normUtil)
                        + w3 * normCost
                        + w4 * normRT
                        + 0.1 * normImbalance;
    }

    // =========================================================
    // NORMALIZATION
    // =========================================================

    private double normalizeMin(
            double value,
            double min,
            double max
    )
    {
        if(max <= min) return 0;

        return (value - min)
                / (max - min);
    }

    private double normalizeMax(
            double value,
            double min,
            double max
    )
    {
        if(max <= min) return 1;

        return (value - min)
                / (max - min);
    }

    // =========================================================
    // UTILITY
    // =========================================================

    private int[] convertToIntArray(
            double[] arr
    )
    {
        int[] result =
                new int[arr.length];

        for(int i = 0; i < arr.length; i++)
        {
            result[i] =
                    (int)Math.round(arr[i]);

            if(result[i] < 0)
                result[i] = 0;

            if(result[i] >= numVMs)
                result[i] = numVMs - 1;
        }

        return result;
    }

    private double std(int[] arr)
    {
        double mean =
                Arrays.stream(arr)
                        .average()
                        .orElse(0);

        double sum = 0;

        for(int v : arr)
        {
            sum += Math.pow(v - mean, 2);
        }

        return Math.sqrt(sum / arr.length);
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public static double getBestFitness()
    {
        return globalBestFitness;
    }

    public static double[] getFitness_for_logs_convergence()
    {
        return fitness_for_logs_convergence;
    }
}