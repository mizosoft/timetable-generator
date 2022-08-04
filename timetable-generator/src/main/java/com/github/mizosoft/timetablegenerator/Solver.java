package com.github.mizosoft.timetablegenerator;


import com.github.mizosoft.timetablegenerator.Models.Group;
import com.github.mizosoft.timetablegenerator.Models.HardWeights;
import com.github.mizosoft.timetablegenerator.Models.ProblemInstance;
import com.github.mizosoft.timetablegenerator.Models.SoftWeights;
import com.github.mizosoft.timetablegenerator.Models.Teacher;
import com.github.mizosoft.timetablegenerator.Models.TotalCost;
import com.github.mizosoft.timetablegenerator.Models.Weights;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.SplittableRandom;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.stream.Stream;

public final class Solver {
  private final ProblemInstance instance;
  private final Indexer<Teacher> teacherIndexer;
  private final Indexer<Group> groupIndexer;
  private final CostFunction costFunction;
  private final Weights weights;
  private final int elitism;
  private final int populationSize;
  private final int maxIterations;

  private final SplittableGenerator rootRnd;
  private final RandomGenerator[] randoms;
  private final double maxMutationProbability;
  private final int parentCount;
  private final int parallelism;
  private final int simulatedAnnealingIterations;
  private final double initialTemperature;
  private final BiFunction<Integer, Double, Double> mutationProbabilityUpdate;

  private double mutationProbability;

  Solver(
      ProblemInstance instance, Weights weights, int populationSize,int elitism, int maxIterations,
      double initialMutationProbability,
      double maxMutationProbability, BiFunction<Integer, Double, Double> mutationProbabilityUpdate,
      int parentCount, int parallelism, int seed, int simulatedAnnealingIterations,
      double initialTemperature) {
    this.instance = instance;
    this.teacherIndexer = new HashIndexer<>(instance.teachers());
    this.groupIndexer = new HashIndexer<>(instance.groups());
    this.costFunction = new CostFunction(instance, teacherIndexer, groupIndexer);
    this.weights = weights;
    this.elitism = elitism;
    this.populationSize = populationSize;
    this.maxIterations = maxIterations;
    this.maxMutationProbability = maxMutationProbability;
    this.parentCount = parentCount;
    this.mutationProbabilityUpdate = mutationProbabilityUpdate;
    this.parallelism = parallelism;
    this.simulatedAnnealingIterations = simulatedAnnealingIterations;
    this.initialTemperature = initialTemperature;
    mutationProbability = initialMutationProbability;
    rootRnd = new SplittableRandom(seed);
    randoms =
        rootRnd
            .splits(parallelism)
            .toArray(RandomGenerator[]::new);
  }

  private record Individual(int[][] table, TotalCost cost) {}

  private final class InitializePopulationTask implements Runnable {
    private final Individual[] population;
    private final int from, to;
    private final RandomGenerator rnd;

    InitializePopulationTask(
        Individual[] population, int from, int to, RandomGenerator rnd) {
      this.population = population;
      this.from = from;
      this.to = Math.min(to, populationSize);
      this.rnd = rnd;
    }

    @Override
    public void run() {
      for (int i = from; i < to; i++) {
        var table =  constructTimetable(rnd, 0.1);
        population[i] = new Individual(table, costFunction.computeTotalCost(table));
      }
    }
  }

  private enum MatingStrategy {
    RANDOM, ROULETTE_WHEEL
  }

  private enum MutationStrategy {
    RANDOM, SIMULATED_ANNEALING
  }

  private final class GenerateOffspringTask implements Runnable {
    private final Individual[] currentPopulation;
    private final Individual[] nextPopulation;
    private final double[] cdf;
    private final int from, to;
    private final RandomGenerator rnd;
    private final MatingStrategy matingStrategy;
    private final MutationStrategy mutationStrategy;

    GenerateOffspringTask(Individual[] currentPopulation, Individual[] nextPopulation, double[] cdf, int from, int to,
        RandomGenerator rnd, MatingStrategy matingStrategy, MutationStrategy mutationStrategy) {
      this.currentPopulation = currentPopulation;
      this.nextPopulation = nextPopulation;
      this.cdf = cdf;
      this.from = from;
      this.to = Math.min(to, populationSize);
      this.rnd = rnd;
      this.matingStrategy = matingStrategy;
      this.mutationStrategy = mutationStrategy;
    }

    private int[] getSelections(Individual[] parents) {
      var selections = new int[instance.groupCount()];
      switch (matingStrategy) {
        case RANDOM -> {
          for (int group = 0; group < instance.groupCount(); group++) {
            selections[group] = rnd.nextInt(parents.length);
          }
        }

        case ROULETTE_WHEEL -> {
          var costs = new int[parentCount];
          int ceiling = 0;
          for (int i = 0; i < parentCount; i++) {
            costs[i] = parents[i].cost().total(weights);
            ceiling = Math.max(costs[i], ceiling);
          }

          // Don't allow zero fitness
          ceiling += Math.max(1, (int) Math.ceil(ceiling * 0.1));

          double sum = 0.0;
          var fitness = new int[parentCount];
          for (int j = 0; j < parentCount; j++) {
            fitness[j] = ceiling - costs[j];
            sum += fitness[j];
          }

          var cdf = new double[parentCount + 1];
          for (int j = 1; j < cdf.length; j++) {
            cdf[j] = cdf[j - 1] + fitness[j - 1] / sum;
          }

          for (int group = 0; group < instance.groupCount(); group++) {
            selections[group] = rouletteWheel(cdf, rnd);
          }
        }
      }
      return selections;
    }

    @Override
    public void run() {
      var parents = new Individual[parentCount];
      for (int i = from; i < to; i++) {
        for (int j = 0; j < parentCount; j++) {
          parents[j] = currentPopulation[rouletteWheel(cdf, rnd)];
        }

        var selections = getSelections(parents);
        var offspring = new int[instance.periodCount()][instance.groupCount()];
        for (int period = 0; period < instance.periodCount(); period++) {
          for (int group = 0; group < instance.groupCount(); group++) {
            offspring[period][group] = parents[selections[group]].table()[period][group];
          }
        }

        switch (mutationStrategy) {
          case RANDOM -> mutate(offspring, rnd);
          case SIMULATED_ANNEALING -> offspring = simulatedAnnealing(offspring, simulatedAnnealingIterations, initialTemperature, rnd);
        }

        nextPopulation[i] = new Individual(offspring, costFunction.computeTotalCost(offspring));
      }
    }
  }

  private void initializePopulation(Individual[] population, Executor pool) {
    int opsPerTask = populationSize / parallelism;
    int rem = populationSize % parallelism;

    var futures = new CompletableFuture<?>[parallelism];
    for (int i = 0, j = 0; i < populationSize; i += opsPerTask, j++) {
      int extra = Math.min(rem, 1);
      futures[j] =
          CompletableFuture.runAsync(
              new InitializePopulationTask(population, i, i + opsPerTask + extra, randoms[j]), pool);
      i += extra;
      rem -= extra;
    }

    CompletableFuture.allOf(futures).join();
  }

  private int[][] constructTimetable(RandomGenerator rnd, double alpha) {
    var table = new int[instance.periodCount()][instance.groupCount()];
    for (var row : table) {
      Arrays.fill(row, -1);
    }

    record LessonPair(int group, int teacher) {}

    var lessons = new HashMap<LessonPair, Integer>();
    for (var lesson : instance.lessons()) {
      lessons.put(
          new LessonPair(
              groupIndexer.indexOf(lesson.group()),
              teacherIndexer.indexOf(lesson.teacher())),
          instance.weeklyOccurrences().get(lesson));
    }

    int totalUnscheduledCount = 0;
    var unscheduledCount = new int[instance.groupCount()][instance.teacherCount()];
    for (var lesson : lessons.keySet()) {
      int needed = lessons.get(lesson);
      unscheduledCount[lesson.group][lesson.teacher] = needed;
      totalUnscheduledCount += needed;
    }

    var isGroupAvailable = new boolean[instance.groupCount()][instance.periodCount()];
    for (var row : isGroupAvailable) {
      Arrays.fill(row, true);
    }

    var isTeacherAvailable = new boolean[instance.teacherCount()][instance.periodCount()];
    for (int teacher = 0; teacher < instance.teacherCount(); teacher++) {
      for (int period = 0; period < instance.periodCount(); period++) {
        isTeacherAvailable[teacher][period] = !costFunction.isTeacherUnavailable(teacher, period);
      }
    }

    var urgency = new double[instance.groupCount()][instance.teacherCount()];
    for (int i = 0; i < totalUnscheduledCount; i++) {
      double minUrgency = 1e9;
      double maxUrgency = -1;
      for (var lesson : lessons.keySet()) {
        if (unscheduledCount[lesson.group][lesson.teacher] == 0) {
          urgency[lesson.group][lesson.teacher] = 0;
          continue;
        }

        int intersections = 0;
        for (int period = 0; period < instance.periodCount(); period++) {
          if (isGroupAvailable[lesson.group][period] && isTeacherAvailable[lesson.teacher][period]) {
            intersections++;
          }
        }

        double u  = 1.0 * unscheduledCount[lesson.group][lesson.teacher] / (intersections + 1);
        urgency[lesson.group][lesson.teacher] = u;
        minUrgency = Math.min(minUrgency, u);
        maxUrgency = Math.max(maxUrgency, u);
      }

      var candidates = new ArrayList<LessonPair>();
      double threshold = maxUrgency - alpha * (maxUrgency - minUrgency);
      for (var lesson : lessons.keySet()) {
        if (urgency[lesson.group][lesson.teacher] >= threshold) {
          candidates.add(lesson);
        }
      }

      var chosenLesson = candidates.get(rnd.nextInt(candidates.size()));

      var freePeriods = new ArrayList<Integer>();
      for (int period = 0; period < instance.periodCount(); period++) {
        if (isGroupAvailable[chosenLesson.group][period] && isTeacherAvailable[chosenLesson.teacher][period]) {
          freePeriods.add(period);
        }
      }

      if (freePeriods.isEmpty()) {
        // Choose any free period for the group without necessarily avoiding teacher conflict
        for (int period = 0; period < instance.periodCount(); period++) {
          if (isGroupAvailable[chosenLesson.group][period]) {
            freePeriods.add(period);
          }
        }
      }

      int chosenPeriod = freePeriods.get(rnd.nextInt(freePeriods.size()));

      assert table[chosenPeriod][chosenLesson.group] != -1;

      table[chosenPeriod][chosenLesson.group] = chosenLesson.teacher;
      unscheduledCount[chosenLesson.group][chosenLesson.teacher]--;
      isGroupAvailable[chosenLesson.group][chosenPeriod] = false;
      isTeacherAvailable[chosenLesson.teacher][chosenPeriod] = false;
    }

    return table;
  }

  private Individual[] generateNextPopulation(
      Individual[] currentPopulation, double[] cdf, MatingStrategy matingStrategy, MutationStrategy mutationStrategy,
      Executor pool) {
    // Pick the elite
    var elite = Stream.of(currentPopulation)
        .sorted(Comparator.comparing(Individual::cost, Comparator.comparingInt(cost -> cost.total(weights))))
        .limit(elitism)
        .toArray(Individual[]::new);

    var nextPopulation = new Individual[populationSize];

    // The elite survives
    System.arraycopy(elite, 0, nextPopulation, 0, elite.length);

    int remaining = populationSize - elitism;
    int opsPerTask = remaining / parallelism;
    int rem = remaining % parallelism;

    var futures = new CompletableFuture<?>[parallelism];
    for (int i = elitism, j = 0; i < populationSize; i += opsPerTask, j++) {
      int extra = Math.min(rem, 1);
      futures[j] =
          CompletableFuture.runAsync(
              new GenerateOffspringTask(
                  currentPopulation, nextPopulation, cdf, i, i + opsPerTask + extra, randoms[j], matingStrategy, mutationStrategy), pool);
      i += extra;
      rem -= extra;
    }

    CompletableFuture.allOf(futures).join();

    return nextPopulation;
  }

  private void mutate(int[][] table, RandomGenerator rnd) {
    int group = rnd.nextInt(instance.groupCount());
    if (rnd.nextDouble() < mutationProbability) {
      int fromPeriod = rnd.nextInt(instance.periodCount());
      int toPeriod = rnd.nextInt(instance.periodCount());

      int temp = table[fromPeriod][group];
      table[fromPeriod][group] = table[toPeriod][group];
      table[toPeriod][group] = temp;
    }
  }

  Individual[] run() {
    var pool =
        new ThreadPoolExecutor(
            parallelism, parallelism, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(parallelism), runnable -> {
              var t = new Thread(runnable);
              t.setDaemon(true);
              return t;
        });

    var population = new Individual[populationSize];

    initializePopulation(population, pool);

    for (int i = 0; i < maxIterations; i++) {
      int ceiling = Stream.of(population)
          .mapToInt(individual -> individual.cost().total(weights))
          .max()
          .orElseThrow();

      // Don't allow zero fitness
      ceiling += Math.max(1, (int) Math.ceil(ceiling * 0.1));

      double sum = 0.0;
      var fitness = new int[populationSize];
      for (int j = 0; j < populationSize; j++) {
        fitness[j] = ceiling - population[j].cost().total(weights);
        sum += fitness[j];
      }

      var cdf = new double[populationSize + 1];
      for (int j = 1; j < cdf.length; j++) {
        cdf[j] = cdf[j - 1] + fitness[j - 1] / sum;
      }

      var matingStrategy = i % 2 == 0 ? MatingStrategy.ROULETTE_WHEEL : MatingStrategy.RANDOM;
      var mutationStrategy = rootRnd.nextDouble() < 0.1 ? MutationStrategy.SIMULATED_ANNEALING : MutationStrategy.RANDOM;
      population = generateNextPopulation(
          population,
          cdf,
          matingStrategy,
          mutationStrategy,
          pool);

      mutationProbability = Math.min(
          maxMutationProbability, mutationProbabilityUpdate.apply(i, mutationProbability));

      if ((i % 200) == 0) {
        System.out.println("Min cost (iter " + i + "): " +
            Stream.of(population)
                .map(Individual::cost)
                .min(Comparator.comparingInt(cost -> cost.total(weights)))
                .orElseThrow());
      }
    }
    return population;
  }

  private int rouletteWheel(double[] cdf, RandomGenerator rnd) {
    double p = rnd.nextDouble();

    int selected = 0;
    for (int j = 1; j < cdf.length - 1 && p > cdf[j]; j++) {
      selected = j;
    }

    if (p > cdf[selected + 1] || p <= cdf[selected]) {
      throw new RuntimeException("bruh");
    }

    return selected;
  }

  private int[][] simulatedAnnealing(int[][] table, int iterations, double initialTemperature, RandomGenerator rnd) {
    var mutatedTable = new int[instance.periodCount()][instance.groupCount()];
    for (int period = 0; period < instance.periodCount(); period++) {
      System.arraycopy(table[period], 0, mutatedTable[period], 0, instance.groupCount());
    }

    for (int i = 0; i < iterations; i++) {
      var cost = costFunction.computeTotalCost(mutatedTable);

      int group = rnd.nextInt(instance.groupCount());
      int fromPeriod = rnd.nextInt(instance.periodCount());
      int toPeriod = rnd.nextInt(instance.periodCount());

      int temp = mutatedTable[fromPeriod][group];
      mutatedTable[fromPeriod][group] = mutatedTable[toPeriod][group];
      mutatedTable[toPeriod][group] = temp;

      var updatedCost = costFunction.computeTotalCost(mutatedTable);

      int delta = updatedCost.total(weights) - cost.total(weights);
      double p = Math.exp(-delta / (initialTemperature / (1 + i)));
      if (delta >= 0
          && (delta == 0 || !(rnd.nextDouble() < p))
          && (delta != 0 || !(rnd.nextDouble() < 0.5))) {
            // Reverse the mutation
          int temp2 = mutatedTable[toPeriod][group];
          mutatedTable[toPeriod][group] = mutatedTable[fromPeriod][group];
          mutatedTable[fromPeriod][group] = temp2;
        }
    }
    return mutatedTable;
  }

  public static void main(String[] args) {
    var weights = new Weights(new HardWeights(200.0, 200.0, 200.0, 200.0, 200.0), new SoftWeights(2.0, 4.0));
    var ga =
        new Solver(
            Samples.readInstance("NE-CESVP-2011-M-D.xml"),
            weights,
            64,
            1,
            8_000,
            0.0005,
            0.005,
            (__, prob) -> prob * 1.0005,
            3,
            8,
            69,
            60,
            60.0);

    var result = ga.run();
    var best = Stream.of(result).min(Comparator.comparing(Individual::cost, Comparator.comparingInt(cost -> cost.total(weights)))).orElseThrow();
    System.out.println("Best cost: " + best.cost());

    ga.printTable(best.table());
  }

  private void printTable(int[][] table) {
    for (int day = 0; day < instance.dayCount(); day++) {
      if (day != 0) {
        System.out.println();
      }

      System.out.println("Day: " + day);

      System.out.print("    ");
      for (int slot = 0; slot < instance.slotCount(); slot++) {
        System.out.printf("%4d", slot);
      }
      System.out.println();

      for (int group = 0; group < instance.groupCount(); group++) {
        System.out.printf("%4d", group);
        for (int slot = 0; slot < instance.slotCount(); slot++) {
          System.out.printf("%4d", table[day * instance.slotCount() + slot][group]);
        }
        System.out.println();
      }
    }
  }
}
