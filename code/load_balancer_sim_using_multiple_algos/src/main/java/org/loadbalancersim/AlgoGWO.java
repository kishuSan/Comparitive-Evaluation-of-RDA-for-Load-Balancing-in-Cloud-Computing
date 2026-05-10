package org.loadbalancersim;

import java.util.Arrays;
import java.util.Random;

public class AlgoGWO {

    private final int iterations;
    private final int populationSize;

    private final int numVMs;
    private final int numCloudlets;

    private final double[] cloudletLengths;
    private final double[] vmMips;
    private final double[] vmCostRate;
    private final double[] vmAvailability;

    private final double w1,w2,w3,w4;

    private double[][] wolves;

    private double[] alphaWolf;
    private double alphaFitness =
            Double.MAX_VALUE;

    private static double[] fitness_for_logs_convergence;

    private static double globalBestFitness;

    private final Random random =
            new Random();

    public AlgoGWO(
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

        initializeWolves();
    }

    private void initializeWolves()
    {
        wolves =
                new double[populationSize][numCloudlets];

        alphaWolf =
                new double[numCloudlets];

        fitness_for_logs_convergence =
                new double[iterations];

        for(int i = 0; i < populationSize; i++)
        {
            for(int j = 0; j < numCloudlets; j++)
            {
                wolves[i][j] =
                        random.nextInt(numVMs);
            }

            double fitness =
                    calculateFitness(
                            convertToIntArray(
                                    wolves[i]
                            )
                    );

            if(fitness < alphaFitness)
            {
                alphaFitness = fitness;

                alphaWolf =
                        Arrays.copyOf(
                                wolves[i],
                                numCloudlets
                        );
            }
        }

        globalBestFitness = alphaFitness;
    }

    public int[] run()
    {
        for(int it = 0; it < iterations; it++)
        {
            double a =
                    2.0
                            - (2.0 * it / iterations);

            for(int i = 0; i < populationSize; i++)
            {
                for(int j = 0; j < numCloudlets; j++)
                {
                    double r1 =
                            random.nextDouble();

                    double r2 =
                            random.nextDouble();

                    double A =
                            2 * a * r1 - a;

                    double C =
                            2 * r2;

                    double D =
                            Math.abs(
                                    C * alphaWolf[j]
                                            - wolves[i][j]
                            );

                    wolves[i][j] =
                            alphaWolf[j]
                                    - A * D;

                    if(wolves[i][j] < 0)
                        wolves[i][j] = 0;

                    if(wolves[i][j] >= numVMs)
                        wolves[i][j] =
                                numVMs - 1;
                }

                int[] solution =
                        convertToIntArray(
                                wolves[i]
                        );

                double fitness =
                        calculateFitness(solution);

                if(fitness < alphaFitness)
                {
                    alphaFitness = fitness;

                    alphaWolf =
                            Arrays.copyOf(
                                    wolves[i],
                                    numCloudlets
                            );
                }
            }

            fitness_for_logs_convergence[it] =
                    alphaFitness;

            System.out.println(
                    "GWO Iteration "
                            + it
                            + " Best Fitness: "
                            + alphaFitness
            );
        }

        globalBestFitness = alphaFitness;

        return convertToIntArray(alphaWolf);
    }

    // SAME FITNESS FUNCTION

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

    public static double getBestFitness()
    {
        return globalBestFitness;
    }

    public static double[] getFitness_for_logs_convergence()
    {
        return fitness_for_logs_convergence;
    }
}