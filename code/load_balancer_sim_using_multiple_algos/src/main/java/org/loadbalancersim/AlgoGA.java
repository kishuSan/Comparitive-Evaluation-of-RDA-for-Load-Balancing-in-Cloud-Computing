package org.loadbalancersim;

import java.util.Arrays;
import java.util.Random;

public class AlgoGA {

    private final int iterations;
    private final int populationSize;

    private final int numVMs;
    private final int numCloudlets;

    private final double[] cloudletLengths;
    private final double[] vmMips;
    private final double[] vmCostRate;
    private final double[] vmAvailability;

    private final double w1, w2, w3, w4;

    private int[][] population;

    private int[] bestSolution;

    private double bestFitness =
            Double.MAX_VALUE;

    private static double[] fitness_for_logs_convergence;

    private static double globalBestFitness;

    private final Random random =
            new Random();

    public AlgoGA(
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

        initializePopulation();
    }

    private void initializePopulation()
    {
        population =
                new int[populationSize][numCloudlets];

        fitness_for_logs_convergence =
                new double[iterations];

        bestSolution =
                new int[numCloudlets];

        for(int i = 0; i < populationSize; i++)
        {
            for(int j = 0; j < numCloudlets; j++)
            {
                population[i][j] =
                        random.nextInt(numVMs);
            }

            double fitness =
                    calculateFitness(population[i]);

            if(fitness < bestFitness)
            {
                bestFitness = fitness;

                bestSolution =
                        Arrays.copyOf(
                                population[i],
                                numCloudlets
                        );
            }
        }

        globalBestFitness = bestFitness;
    }

    public int[] run()
    {
        for(int it = 0; it < iterations; it++)
        {
            int[][] newPopulation =
                    new int[populationSize][numCloudlets];

            for(int i = 0; i < populationSize; i++)
            {
                int[] parent1 = tournamentSelection();
                int[] parent2 = tournamentSelection();

                int[] child =
                        crossover(parent1, parent2);

                mutate(child);

                newPopulation[i] = child;

                double fitness =
                        calculateFitness(child);

                if(fitness < bestFitness)
                {
                    bestFitness = fitness;

                    bestSolution =
                            Arrays.copyOf(
                                    child,
                                    numCloudlets
                            );
                }
            }

            population = newPopulation;

            fitness_for_logs_convergence[it] =
                    bestFitness;

            System.out.println(
                    "GA Iteration "
                            + it
                            + " Best Fitness: "
                            + bestFitness
            );
        }

        globalBestFitness = bestFitness;

        return bestSolution;
    }

    private int[] tournamentSelection()
    {
        int a =
                random.nextInt(populationSize);

        int b =
                random.nextInt(populationSize);

        double fa =
                calculateFitness(population[a]);

        double fb =
                calculateFitness(population[b]);

        return fa < fb
                ? population[a]
                : population[b];
    }

    private int[] crossover(
            int[] p1,
            int[] p2
    )
    {
        int[] child =
                new int[numCloudlets];

        int point =
                random.nextInt(numCloudlets);

        for(int i = 0; i < numCloudlets; i++)
        {
            child[i] =
                    (i < point)
                            ? p1[i]
                            : p2[i];
        }

        return child;
    }

    private void mutate(int[] child)
    {
        double mutationRate = 0.05;

        for(int i = 0; i < numCloudlets; i++)
        {
            if(random.nextDouble()
                    < mutationRate)
            {
                child[i] =
                        random.nextInt(numVMs);
            }
        }
    }

    // SAME FITNESS AS OTHER ALGOS

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

        double makespan =
                Arrays.stream(vmExecutionTime)
                        .max()
                        .getAsDouble();

        double avgRT =
                Arrays.stream(vmExecutionTime)
                        .average()
                        .orElse(0);

        double totalBusy =
                Arrays.stream(vmExecutionTime)
                        .sum();

        double utilization =
                totalBusy
                        / (numVMs * makespan);

        double imbalance =
                std(vmLoad);

        double normMakespan =
                normalizeMin(makespan,0,10000);

        double normRT =
                normalizeMin(avgRT,0,10000);

        double normCost =
                normalizeMin(totalCost,0,10000);

        double normUtil =
                normalizeMax(utilization,0,1);

        double normImbalance =
                normalizeMin(imbalance,0,100);

        return
                w1 * normMakespan
                        + w2 * (1 - normUtil)
                        + w3 * normCost
                        + w4 * normRT
                        + 0.1 * normImbalance;
    }

    private double normalizeMin(
            double value,
            double min,
            double max
    ) {
        return (value - min)
                / (max - min);
    }

    private double normalizeMax(
            double value,
            double min,
            double max
    ) {
        return (value - min)
                / (max - min);
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

    public static double getBestFitness()
    {
        return globalBestFitness;
    }

    public static double[] getFitness_for_logs_convergence()
    {
        return fitness_for_logs_convergence;
    }
}