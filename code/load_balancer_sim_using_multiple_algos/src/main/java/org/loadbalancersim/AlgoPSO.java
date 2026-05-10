package org.loadbalancersim;

import java.util.Arrays;
import java.util.Random;

/**
 * Particle Swarm Optimization (PSO)
 * for Cloudlet-to-VM Scheduling
 *
 * Reuses SAME fitness function structure as RDA
 * for fair comparison.
 */
public class AlgoPSO {

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

    // PSO coefficients
    private final double inertiaWeight = 0.7;
    private final double cognitiveCoeff = 1.5;
    private final double socialCoeff = 1.5;

    // Particle positions and velocities
    private double[][] positions;
    private double[][] velocities;

    // Personal best
    private double[][] personalBestPositions;
    private double[] personalBestFitness;

    // Global best
    private double[] globalBestPosition;
    private double globalBestFitness =
            Double.MAX_VALUE;

    // Convergence logging
    private static double[] fitness_for_logs_convergence;

    private static double bestFitness;

    private final Random random = new Random();

    public AlgoPSO(
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

        initializeParticles();
    }

    // =========================================================
    // INITIALIZE PARTICLES
    // =========================================================

    private void initializeParticles()
    {
        positions =
                new double[populationSize][numCloudlets];

        velocities =
                new double[populationSize][numCloudlets];

        personalBestPositions =
                new double[populationSize][numCloudlets];

        personalBestFitness =
                new double[populationSize];

        globalBestPosition =
                new double[numCloudlets];

        fitness_for_logs_convergence =
                new double[iterations];

        for(int i = 0; i < populationSize; i++)
        {
            for(int j = 0; j < numCloudlets; j++)
            {
                positions[i][j] =
                        random.nextInt(numVMs);

                velocities[i][j] =
                        random.nextDouble();

                personalBestPositions[i][j] =
                        positions[i][j];
            }

            double fitness =
                    calculateFitness(
                            convertToIntArray(
                                    positions[i]
                            )
                    );

            personalBestFitness[i] =
                    fitness;

            if(fitness < globalBestFitness)
            {
                globalBestFitness = fitness;

                globalBestPosition =
                        Arrays.copyOf(
                                positions[i],
                                numCloudlets
                        );
            }
        }

        bestFitness = globalBestFitness;
    }

    // =========================================================
    // MAIN PSO LOOP
    // =========================================================

    public int[] run()
    {
        for(int it = 0; it < iterations; it++)
        {
            for(int i = 0; i < populationSize; i++)
            {
                updateVelocity(i);

                updatePosition(i);

                int[] particle =
                        convertToIntArray(
                                positions[i]
                        );

                double fitness =
                        calculateFitness(
                                particle
                        );

                // Personal best
                if(fitness < personalBestFitness[i])
                {
                    personalBestFitness[i] =
                            fitness;

                    personalBestPositions[i] =
                            Arrays.copyOf(
                                    positions[i],
                                    numCloudlets
                            );
                }

                // Global best
                if(fitness < globalBestFitness)
                {
                    globalBestFitness =
                            fitness;

                    globalBestPosition =
                            Arrays.copyOf(
                                    positions[i],
                                    numCloudlets
                            );
                }
            }

            fitness_for_logs_convergence[it] =
                    globalBestFitness;

            System.out.println(
                    "PSO Iteration "
                            + it
                            + " Best Fitness: "
                            + globalBestFitness
            );
        }

        bestFitness = globalBestFitness;

        return convertToIntArray(
                globalBestPosition
        );
    }

    // =========================================================
    // VELOCITY UPDATE
    // =========================================================

    private void updateVelocity(int particleIndex)
    {
        for(int j = 0; j < numCloudlets; j++)
        {
            double r1 = random.nextDouble();
            double r2 = random.nextDouble();

            velocities[particleIndex][j] =
                    inertiaWeight
                            * velocities[particleIndex][j]

                            + cognitiveCoeff
                            * r1
                            * (
                            personalBestPositions[particleIndex][j]
                                    - positions[particleIndex][j]
                    )

                            + socialCoeff
                            * r2
                            * (
                            globalBestPosition[j]
                                    - positions[particleIndex][j]
                    );
        }
    }

    // =========================================================
    // POSITION UPDATE
    // =========================================================

    private void updatePosition(int particleIndex)
    {
        for(int j = 0; j < numCloudlets; j++)
        {
            positions[particleIndex][j] +=
                    velocities[particleIndex][j];

            // Clamp
            if(positions[particleIndex][j] < 0)
            {
                positions[particleIndex][j] = 0;
            }

            if(positions[particleIndex][j]
                    >= numVMs)
            {
                positions[particleIndex][j] =
                        numVMs - 1;
            }
        }
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

        // Response time
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

        // Normalize
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
        return bestFitness;
    }

    public static double[] getFitness_for_logs_convergence()
    {
        return fitness_for_logs_convergence;
    }
}