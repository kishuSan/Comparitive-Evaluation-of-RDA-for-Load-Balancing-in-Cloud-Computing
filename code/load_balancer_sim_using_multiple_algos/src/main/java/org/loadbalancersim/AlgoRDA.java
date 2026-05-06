package org.loadbalancersim;

import java.util.*;

class Deer {
    int index;
    double[] position;
    double fitness;
    QoSMetrics rawMetrics;

    Deer(int var1, double[] var2, double var3) {
        this.index = var1;
        this.position = var2;
        this.fitness = var3;
    }
}

public class AlgoRDA {
    private final int numPopulation;
    private final int numMales;
    private final int numHinds;
    private final int numStags;
    private final int numCommanders;
    private final int numIterations;
    private final int numVMs;
    private final int numCloudlets;
    private int uniID;
    private double bestFitness;
    private double[] bestPosition;
    private List<Deer> allSolutions;
    private List<Deer> males;
    private List<Deer> hinds;
    private List<Deer> commanders;
    private List<Deer> stags;
    private final List<Deer> fitnessPool;
    private double alpha;
    private double beta;
    private double gamma;
    private final double UB;
    private final double LB;

    private FitnessFunction fitnessFunction;

    void set_alpha(float var1) {
        this.alpha = (double) var1;
    }

    void set_gamma(float var1) {
        this.gamma = (double) var1;
    }

    void set_beta(float var1) {
        this.beta = (double) var1;
    }

    public AlgoRDA(
        int numVMs,
        int numCloudlets_requests,
        double[] cloudletLength,
        double[] vmMips,
        double[] vmCostRate,
        double[] vmAvailability,
        double w1, double w2, double w3, double w4
    ) {
        this.numIterations = 100;
        this.numPopulation = 100;
        this.numMales = Math.max(1, (int) Math.round(this.numPopulation * (double).20F));
        this.numHinds = this.numPopulation - this.numMales;
        this.uniID = 0;
        this.bestFitness = Double.MAX_VALUE;
        this.males = new ArrayList<Deer>();
        this.hinds = new ArrayList<Deer>();
        this.commanders = new ArrayList<Deer>();
        this.stags = new ArrayList<Deer>();
        this.allSolutions = new ArrayList<Deer>();
        this.fitnessPool = new ArrayList<Deer>();
        this.alpha = 0.5F;
        this.beta = 0.2;
        this.gamma = 0.4;
        this.LB = 0.0F;

        if (numVMs > 0 && numCloudlets_requests > 0) {
            this.numVMs = numVMs;
            this.numCloudlets = numCloudlets_requests;
            this.fitnessFunction = new FitnessFunction(
                numVMs, numCloudlets_requests,
                cloudletLength, vmMips, vmCostRate, vmAvailability,
                w1, w2, w3, w4
            );
            this.numCommanders = Math.max(1, (int) Math.round(this.gamma * (double) this.numMales));
            this.numStags = this.numMales - this.numCommanders;
            this.bestPosition = new double[numCloudlets_requests];
            this.UB = (double) numVMs-1;
            this.initializePopulation();
        } else {
            throw new IllegalArgumentException("numVMs and numCloudlets must be > 0");
        }
    }

    private void initializePopulation() {
        Random rand = new Random();

        while (this.uniID++ < this.numPopulation) {
            double[] sol = new double[this.numCloudlets];
            // generating a random solution initially
            for (int i = 0; i < this.numCloudlets; ++i) {
                sol[i] = this.LB + (this.UB - this.LB) * rand.nextDouble();
            }

            this.allSolutions.add(new Deer(this.uniID, sol, 0.0));
        }

        // fitness evaluation of entire initial solution
        evaluatePopulation(this.allSolutions);

        this.allSolutions.sort(Comparator.comparingDouble((deer) -> deer.fitness));

        // select the best numMales solutions as the male red deer
        this.males = new ArrayList<>(this.allSolutions.subList(0, this.numMales));
        this.hinds = new ArrayList<>(this.allSolutions.subList(this.numMales, this.numPopulation));
        this.commanders = new ArrayList<>(this.males.subList(0, this.numCommanders));
        this.stags = new ArrayList<>(this.males.subList(this.numCommanders, this.numMales));

        this.updateBestSolution(this.allSolutions);
    }

    public int[] run() {
        int noImprovement = 0;
        double prevBest = Double.MAX_VALUE;

        for (int it = 0; it < this.numIterations; ++it) {
            System.out.println("iter : " + it);

            // bounds from existing rawMetrics — no need to re-evaluate positions
            List<QoSMetrics> currentMetrics = new ArrayList<>();
            for (Deer deer : this.allSolutions) currentMetrics.add(deer.rawMetrics);
            PopulationBounds bounds = fitnessFunction.computeBounds(currentMetrics);

            this.roaringPhase(bounds);
            this.selectCommanders();
            this.fightingPhase(bounds);
            int[][] harems = this.formHarems();
            this.matingPhase(harems, bounds);
            this.selectNextGen();

            this.updateBestSolution(this.allSolutions);

            if (Math.abs(prevBest - bestFitness) < 1e-6) {
                noImprovement++;
            } else {
                noImprovement = 0;
            }

            if (bestFitness <= 1e-6) break;          // near-optimal
            if (noImprovement >= 10) break;          // convergence
        }

        int[] bestVmAssignment = new int[this.numCloudlets];
        System.out.println("best vm assignment : ");
        for (int i = 0; i < this.numCloudlets; ++i) {
            bestVmAssignment[i] = (int) this.bestPosition[i];
            System.out.print((int) this.bestPosition[i] + " ");
        }
        System.out.println("=======");
        return bestVmAssignment;
    }

    private void evaluatePopulation(List<Deer> population) {

        // pass 1 — raw QoS metrics for every deer
        List<QoSMetrics> rawMetrics = new ArrayList<>();
        for (Deer deer : population) {
            QoSMetrics metrics = fitnessFunction.evaluateRaw(deer.position);
            deer.rawMetrics = metrics;
            rawMetrics.add(metrics);
        }

        // compute population-wide min/max bounds
        PopulationBounds bounds = fitnessFunction.computeBounds(rawMetrics);

        // pass 2 — normalize and assign final scalar fitness
        for (Deer deer : population) {
            deer.fitness = fitnessFunction.normalizeAndScore(deer.rawMetrics, bounds);
        }
    }

    private void roaringPhase(PopulationBounds bounds) {
        Random rand = new Random();

        for (Deer male : this.males) {
            double a1 = rand.nextDouble();
            double a2 = rand.nextDouble();
            double a3 = rand.nextDouble();
            double[] newPosition = new double[this.numCloudlets];

            for (int i = 0; i < this.numCloudlets; ++i) {
                double offset = a1 * (a2 * (this.UB - this.LB) + this.LB);
                double newVal = (a3 >= 0.5) ? male.position[i] + offset : male.position[i] - offset;
                newPosition[i] = Math.max(this.LB, Math.min(this.UB, newVal));
            }

            // normalize challenger using the SAME bounds as the current population
            QoSMetrics challengerMetrics = fitnessFunction.evaluateRaw(newPosition);
            double challengerFitness = fitnessFunction.normalizeAndScore(challengerMetrics, bounds);

            // now this comparison is valid — both scores on the same [0,1] scale
            if (challengerFitness < male.fitness) {
                male.position   = newPosition;
                male.fitness    = challengerFitness;
                male.rawMetrics = challengerMetrics;
            }
        }
    }

    private void fightingPhase(PopulationBounds bounds) {
        Random rand = new Random();

        for (Deer commander : this.commanders) {
            Deer randStag = (Deer) this.stags.get(rand.nextInt(this.numStags));
            double b1 = rand.nextDouble();
            double b2 = rand.nextDouble();
            double[] newSol1 = new double[this.numCloudlets];
            double[] newSol2 = new double[this.numCloudlets];

            for (int i = 0; i < this.numCloudlets; ++i) {
                double avg = (commander.position[i] + randStag.position[i]) / 2.0;
                double offset = b1 * ( b2 * (this.UB - this.LB) + this.LB);
                newSol1[i] = Math.max(this.LB, Math.min(this.UB, avg + offset));
                newSol2[i] = Math.max(this.LB, Math.min(this.UB, avg - offset));
            }

            QoSMetrics m1 = fitnessFunction.evaluateRaw(newSol1);
            QoSMetrics m2 = fitnessFunction.evaluateRaw(newSol2);
            double f1 = fitnessFunction.normalizeAndScore(m1, bounds);
            double f2 = fitnessFunction.normalizeAndScore(m2, bounds);

            // pick the better candidate between f1 and f2 first
            double[] bestNewPosition = (f1 <= f2) ? newSol1 : newSol2;
            QoSMetrics bestNewMetrics = (f1 <= f2) ? m1 : m2;
            double bestNewFitness = Math.min(f1, f2);

            // then check if that winner actually beats the commander
            if (bestNewFitness < commander.fitness) {
                commander.position   = bestNewPosition;
                commander.fitness    = bestNewFitness;
                commander.rawMetrics = bestNewMetrics;
            }
        }
    }

    private int[][] formHarems() {
        System.out.println("Harem Formation:");
        int[][] commanders = new int[this.numCommanders][];
        double[] normalizedCommanderFitness = new double[this.numCommanders];
        double maxFitness_amongCommanders = Double.MIN_VALUE;

        for (int i = 0; i < this.numCommanders; ++i) {
            if (((Deer) this.commanders.get(i)).fitness > maxFitness_amongCommanders) {
                maxFitness_amongCommanders = ((Deer) this.commanders.get(i)).fitness;
            }
        }

        double aggregateSumOfNormalizedFitness = (double) 0.0F;

        for (int i = 0; i < this.numCommanders; ++i) {
            normalizedCommanderFitness[i] = Math.abs(((Deer) this.commanders.get(i)).fitness - maxFitness_amongCommanders);
            aggregateSumOfNormalizedFitness += normalizedCommanderFitness[i];
        }

        ArrayList<Integer> poolOfHinds = new ArrayList<>();

        for (Deer hind : this.hinds) {
            poolOfHinds.add(hind.index);
        }

        Collections.shuffle(poolOfHinds);
        int numOfHindsAssigned = 0;

        for (int i = 0; i < this.numCommanders; ++i) {
            double proportion = normalizedCommanderFitness[i] / aggregateSumOfNormalizedFitness;
            int numHinds_ithCommander = (i == this.numCommanders - 1) ? this.numHinds - numOfHindsAssigned  : (int) Math.round(proportion * (double) this.numHinds);
            commanders[i] = new int[numHinds_ithCommander];

            for (int j = 0; j < numHinds_ithCommander && numOfHindsAssigned < this.numHinds; ++j) {
                commanders[i][j] = (Integer) poolOfHinds.get(numOfHindsAssigned++);
                System.out.print(commanders[i][j] + " ");
            }

            System.out.println();
        }

        System.out.println("=====");
        return commanders;
    }

    private void matingPhase(int[][] harems, PopulationBounds bounds) {
        Random rand = new Random();

        for (int i = 0; i < this.numCommanders; ++i) {
            Deer commander = (Deer) this.commanders.get(i);

            int otherHarem = rand.nextInt(numCommanders);
            while(otherHarem == i) otherHarem = rand.nextInt(numCommanders);

            int intraCount = (int) ((double) harems[i].length * this.alpha);
            int interCount = (int) ((double) harems[otherHarem].length * this.beta);

            // Intra-harem mating
            for (int j = 0; j < intraCount; ++j) {
                if (harems[i].length != 0) {
                    int hindIdx = harems[i][rand.nextInt(harems[i].length)];
                    this.mate(commander, this.getDeerByIndex(hindIdx), bounds);
                }
            }

            // Inter-harem mating
            for (int j = 0; j < interCount; ++j) {
                if (harems[otherHarem].length != 0) {
                    int hindIdx = harems[otherHarem][rand.nextInt(harems[otherHarem].length)];
                    this.mate(commander, this.getDeerByIndex(hindIdx), bounds);
                }
            }
        }

        // Stag mating
        for (Deer stag : this.stags) {
            Deer nearestHind = this.findNearestHind(stag);
            this.mate(stag, nearestHind, bounds);
        }
    }

    private void mate(Deer male, Deer hind, PopulationBounds bounds) {
        if (hind == null) return;

        Random rand = new Random();
        double c = rand.nextDouble();
        double[] offspring = new double[this.numCloudlets];

        for (int i = 0; i < this.numCloudlets; i++) {
            double val = ((male.position[i] + hind.position[i]) / 2.0) + c * (this.UB - this.LB);
            offspring[i] = Math.max(this.LB, Math.min(this.UB, val));
        }

        QoSMetrics m = fitnessFunction.evaluateRaw(offspring);
        double offspringFitness = fitnessFunction.normalizeAndScore(m, bounds);

        Deer offspringDeer = new Deer(this.uniID++, offspring, offspringFitness);
        offspringDeer.rawMetrics = m;
        this.fitnessPool.add(offspringDeer);
    }

    private Deer getDeerByIndex(int idx) {
        for (Deer hind : this.hinds) {
            if (hind.index == idx) {
                return hind;
            }
        }

        // we only need to find hinds - as usage of this function is specifically only for hinds.
//        for (Deer male : this.males) {
//            if (male.index == idx) {
//                return male;
//            }
//        }

        return null;
    }

    private Deer findNearestHind(Deer male) {
        Deer nearestHind = this.hinds.get(0);
        double overall_best_dist = Double.MAX_VALUE;

        for (Deer curr_hind : this.hinds) {
            double curr_hind_dist = 0.0F;

            for (int i = 0; i < this.numCloudlets; ++i) {
                curr_hind_dist += Math.pow(male.position[i] - curr_hind.position[i], (double) 2.0F);
            }

            curr_hind_dist = Math.sqrt(curr_hind_dist);
            if (curr_hind_dist < overall_best_dist) {
                overall_best_dist = curr_hind_dist;
                nearestHind = curr_hind;
            }
        }

        return nearestHind;
    }

    private void selectNextGen() {
        this.fitnessPool.addAll(this.allSolutions);

        this.fitnessPool.sort(Comparator.comparingDouble(d -> d.fitness));

        ArrayList<Deer> newSolution = new ArrayList<>(this.fitnessPool.subList(0, this.numMales));

        // --- hinds: roulette wheel selection ---
        ArrayList<Deer> hindPool = new ArrayList<>(this.fitnessPool.subList(this.numMales, this.fitnessPool.size()));
        ArrayList<Deer> selectedHinds = rouletteWheelSelect(hindPool, this.numHinds);

        newSolution.addAll(selectedHinds);

        this.fitnessPool.clear();
        this.allSolutions = newSolution;

        this.males      = new ArrayList<>(this.allSolutions.subList(0, this.numMales));
        this.hinds      = new ArrayList<>(this.allSolutions.subList(this.numMales, this.numPopulation));
        this.commanders = new ArrayList<>(this.males.subList(0, this.numCommanders));
        this.stags      = new ArrayList<>(this.males.subList(this.numCommanders, this.numMales));
    }

    private ArrayList<Deer> rouletteWheelSelect(ArrayList<Deer> hindPool, int numToSelect) {
        // Step 1: invert fitness scores (minimization problem — lower fitness is better)
        double[] inverted = new double[hindPool.size()];
        double totalInverted = 0.0;
        for (int i = 0; i < hindPool.size(); i++) {
            double f = hindPool.get(i).fitness;
            inverted[i] = 1.0 / (f > 0.0 ? f : 1e-10);
            totalInverted += inverted[i];
        }

        // Step 2: compute selection probabilities
        double[] probabilities = new double[hindPool.size()];
        for (int i = 0; i < hindPool.size(); i++) {
            probabilities[i] = inverted[i] / totalInverted;
        }

        // Step 3: build cumulative distribution
        double[] cumulative = new double[hindPool.size()];
        cumulative[0] = probabilities[0];
        for (int i = 1; i < hindPool.size(); i++) {
            cumulative[i] = cumulative[i - 1] + probabilities[i];
        }

        // Step 4: spin the wheel numToSelect times (without replacement)
        Random rand = new Random();
        Set<Integer> selectedIndices = new LinkedHashSet<>();
        int attempts = 0;
        int maxAttempts = numToSelect * 100;

        while (selectedIndices.size() < numToSelect && attempts < maxAttempts) {
            double r = rand.nextDouble();
            for (int i = 0; i < cumulative.length; i++) {
                if (r <= cumulative[i]) {
                    selectedIndices.add(i);
                    break;
                }
            }
            attempts++;
        }

        // fallback: if roulette didn't fill slots (e.g. tiny pool), pad with best remaining
        if (selectedIndices.size() < numToSelect) {
            for (int i = 0; i < hindPool.size() && selectedIndices.size() < numToSelect; i++) {
                selectedIndices.add(i);
            }
        }

        ArrayList<Deer> result = new ArrayList<>();
        for (int idx : selectedIndices) {
            result.add(hindPool.get(idx));
        }
        return result;
    }

    private void updateBestSolution(List<Deer> solutions) {
        for (Deer sol : solutions) {
            if (sol.fitness < this.bestFitness) {
                this.bestFitness = sol.fitness;
                this.bestPosition = (double[]) sol.position.clone();
            }
        }
    }

    private void selectCommanders() {
        this.males.sort(Comparator.comparingDouble((deer) -> deer.fitness));
        this.commanders = this.males.subList(0, this.numCommanders);
        this.stags = this.males.subList(this.numCommanders, this.numMales);
    }
}
