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
import java.util.SplittableRandom;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeneticAlgorithm {
  private static final int N_THREADS = 8;

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
  private double mutationProbability = 0.05;

  GeneticAlgorithm(
      ProblemInstance instance, Weights weights, int populationSize,int elitism, int maxIterations,
      double maxMutationProbability) {
    this.instance = instance;
    this.teacherIndexer = new HashIndexer<>(instance.teachers());
    this.groupIndexer = new HashIndexer<>(instance.groups());
    this.costFunction = new CostFunction(instance, teacherIndexer, groupIndexer);
    this.weights = weights;
    this.elitism = elitism;
    this.populationSize = populationSize;
    this.maxIterations = maxIterations;
    this.maxMutationProbability = maxMutationProbability;
    rootRnd = new SplittableRandom();
    randoms =
        rootRnd
            .splits((populationSize + N_THREADS) / N_THREADS)
            .toArray(RandomGenerator[]::new);
  }

  private record Individual(int[][] table, TotalCost cost) {}

  private class InitializePopulationTask implements Runnable {
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
        var table =  generateTable(rnd);
        population[i] = new Individual(table, costFunction.computeTotalCost(table));
      }
    }
  }

  private enum MatingStrategy {
    RANDOM, BEST_GENE, ROULETTE_WHEEL
  }

  private class GenerateOffspringTask implements Runnable {
    private final Individual[] currentPopulation;
    private final Individual[] nextPopulation;
    private final double[] cdf;
    private final int from, to;
    private final RandomGenerator rnd;
    private final MatingStrategy strategy;

    GenerateOffspringTask(Individual[] currentPopulation, Individual[] nextPopulation, double[] cdf, int from, int to,
        RandomGenerator rnd, MatingStrategy strategy) {
      this.currentPopulation = currentPopulation;
      this.nextPopulation = nextPopulation;
      this.cdf = cdf;
      this.from = from;
      this.to = Math.min(to, populationSize);
      this.rnd = rnd;
      this.strategy = strategy;
    }

    private boolean[] getSelections(int[][] firstParent, int[][] secondParent) {
      var selections = new boolean[instance.groupCount()];
      switch (strategy) {
        case BEST_GENE -> {
          for (int group = 0; group < instance.groupCount(); group++) {
            int firstCost = costFunction.computeTotalCostForGroup(firstParent, group).total(weights);
            int secondCost = costFunction.computeTotalCostForGroup(secondParent, group).total(weights);
            selections[group] = firstCost < secondCost || (firstCost == secondCost && rnd.nextDouble() < 0.5);
          }
        }
        case RANDOM -> {
          for (int group = 0; group < instance.groupCount(); group++) {
            selections[group] = rnd.nextDouble() < 0.5;
          }
        }
      }
      return selections;
    }

    @Override
    public void run() {
      for (int i = from; i < to; i++) {
        var firstParent = currentPopulation[rouletteWheel(cdf, rnd)].table();
        var secondParent = currentPopulation[rouletteWheel(cdf, rnd)].table();

        var selections = getSelections(firstParent, secondParent);
        var offspring = new int[instance.periodCount()][instance.groupCount()];
        for (int period = 0; period < instance.periodCount(); period++) {
          for (int group = 0; group < instance.groupCount(); group++) {
            if (selections[group]) {
              offspring[period][group] = firstParent[period][group];
            } else {
              offspring[period][group] = secondParent[period][group];
            }
          }
        }

        // Apply mutations to each group
        for (int group = 0; group < instance.groupCount(); group++) {
          if (rnd.nextDouble() < mutationProbability) {
            int fromPeriod = rnd.nextInt(instance.periodCount());
            int toPeriod = rnd.nextInt(instance.periodCount());

            int temp = offspring[fromPeriod][group];
            offspring[fromPeriod][group] = offspring[toPeriod][group];
            offspring[toPeriod][group] = temp;
          }
        }

        nextPopulation[i] = new Individual(offspring, costFunction.computeTotalCost(offspring));
      }
    }
  }


  private class GenerateOffspringTask1 implements Runnable {
    private final Individual[] currentPopulation;
    private final Individual[] nextPopulation;
    private final double[] cdf;
    private final int from, to;
    private final RandomGenerator rnd;
    private final MatingStrategy strategy;
    private final int parentCount = 6;

    GenerateOffspringTask1(Individual[] currentPopulation, Individual[] nextPopulation, double[] cdf, int from, int to,
        RandomGenerator rnd, MatingStrategy strategy) {
      this.currentPopulation = currentPopulation;
      this.nextPopulation = nextPopulation;
      this.cdf = cdf;
      this.from = from;
      this.to = Math.min(to, populationSize);
      this.rnd = rnd;
      this.strategy = strategy;
    }

    private int[] getSelections(Individual[] parents) {
      var selections = new int[instance.groupCount()];
      switch (strategy) {
        case BEST_GENE -> {
          for (int group = 0; group < instance.groupCount(); group++) {
            int selected = 0;
            int best = costFunction.computeTotalCostForGroup(parents[0].table(), group).total(weights);
            for (int i = 1; i < parents.length; i++) {
              var cost = costFunction.computeTotalCostForGroup(parents[i].table(), group).total(weights);
              if (cost < best) {
                selected = i;
                best = cost;
              }
            }
            selections[group] = selected;
          }
        }
        case RANDOM -> {
          for (int group = 0; group < instance.groupCount(); group++) {
            selections[group] = rnd.nextInt(parents.length);
          }
        }
        case ROULETTE_WHEEL -> {
          var costs = new int[parents.length];
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
      for (int i = from; i < to; i++) {
        var parents = new Individual[parentCount];
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

        // Apply mutations to each group
        for (int group = 0; group < instance.groupCount(); group++) {
          if (rnd.nextDouble() < mutationProbability) {
            int fromPeriod = rnd.nextInt(instance.periodCount());
            int toPeriod = rnd.nextInt(instance.periodCount());

            int temp = offspring[fromPeriod][group];
            offspring[fromPeriod][group] = offspring[toPeriod][group];
            offspring[toPeriod][group] = temp;
          }
        }

        nextPopulation[i] = new Individual(offspring, costFunction.computeTotalCost(offspring));
      }
    }
  }

  private void initializePopulation(Individual[] population, Executor pool) {
    int opsPerTask = (populationSize + N_THREADS) / N_THREADS;

    var futures = new CompletableFuture<?>[N_THREADS];
    for (int i = 0, j = 0; i < populationSize; i += opsPerTask, j++) {
      futures[j] =
          CompletableFuture.runAsync(
              new InitializePopulationTask(population, i, i + opsPerTask, randoms[j]), pool);
    }

    CompletableFuture.allOf(futures).join();
  }

  private Individual[] generateNextPopulation(
      Individual[] currentPopulation, double[] cdf, MatingStrategy matingStrategy, Executor pool) {
    // Pick the elite
    var elite = Stream.of(currentPopulation)
        .sorted(Comparator.comparing(Individual::cost, Comparator.comparingInt(cost -> cost.total(weights))))
        .limit(elitism)
        .toArray(Individual[]::new);

//    System.out.println(Arrays.toString(elite));
//    var elite2 = Stream.of(currentPopulation).map(ind -> ind.cost().total(weights)).sorted().toList();
//    var ogelite = IntStream.of(elite).map(i -> currentPopulation[i].cost().total(weights)).sorted().boxed().toList();
//    elite2 = elite2.subList(0, elitism);
//    if (!elite2.equals(ogelite)) {
//      throw new RuntimeException(elite2 + ", " + ogelite);
//    }



    var nextPopulation = new Individual[populationSize];

    // The elite survives
    System.arraycopy(elite, 0, nextPopulation, 0, elite.length);

    int remaining = populationSize - elitism;
    int opsPerTask = (remaining + N_THREADS) / N_THREADS;

    var futures = new CompletableFuture<?>[N_THREADS];
    for (int i = elitism, j = 0; i < populationSize; i += opsPerTask, j++) {
      futures[j] =
          CompletableFuture.runAsync(
              new GenerateOffspringTask1(
                  currentPopulation, nextPopulation, cdf, i, i + opsPerTask, randoms[j], matingStrategy), pool);
    }

    CompletableFuture.allOf(futures).join();

    return nextPopulation;
  }

  void run() {
    var pool =
        new ThreadPoolExecutor(
            N_THREADS, N_THREADS, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(N_THREADS));

    var population = new Individual[populationSize];

    initializePopulation(population, pool);

    System.out.println(
        Stream.of(population)
            .map(Individual::cost)
            .sorted(Comparator.comparingInt(cost -> cost.total(weights)))
            .limit(10)
            .map(Object::toString)
            .collect(Collectors.joining(", ")));

    for (int i = 0; i < maxIterations; i++) {
      int ceiling = Stream.of(population).mapToInt(individual -> individual.cost().total(weights)).max().orElseThrow();

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

      population = generateNextPopulation(
          population,
          cdf,
          rootRnd.nextDouble() < 0.5 ? MatingStrategy.BEST_GENE : MatingStrategy.ROULETTE_WHEEL,
//          MatingStrategy.ROULETTE_WHEEL,
          pool);

      mutationProbability = Math.min(maxMutationProbability, mutationProbability * 1.0006);

      if ((i % 500) == 0) {
        System.out.println(mutationProbability);
        System.out.println(i);
        System.out.println(
            Stream.of(population)
                .map(Individual::cost)
                .sorted(Comparator.comparingInt(cost -> cost.total(weights)))
                .limit(10)
                .map(Object::toString)
                .collect(Collectors.joining(", ")));
      }
    }
  }

  private int[][] generateTable(RandomGenerator rnd) {
    var timetable = new int[instance.periodCount()][instance.groupCount()];

    for (var row : timetable) {
      Arrays.fill(row, -1);
    }

    var freeSlots = new ArrayList<Integer>();
    for (var lesson : instance.lessons()) {
      int group = groupIndexer.indexOf(lesson.group());
      int lessonCount = instance.weeklyOccurrences().get(lesson);
      for (int i = 0; i < lessonCount; i++) {
        for (int period = 0; period < instance.periodCount(); period++) {
          if (timetable[period][group] == -1) {
            freeSlots.add(period);
          }
        }

        if (freeSlots.isEmpty()) {
          throw new IllegalStateException("bruh");
        }

        timetable[freeSlots.get(rnd.nextInt(freeSlots.size()))][group] =
            teacherIndexer.indexOf(lesson.teacher());
      }
    }
    return timetable;
  }

  private int rouletteWheel(double[] cdf, RandomGenerator rnd) {
    double p = rnd.nextDouble();

    // TODO we can implement this with binary search.
    int selected = 0;
    for (int j = 1; j < cdf.length - 1 && p > cdf[j]; j++) {
      selected = j;
    }

    if (p > cdf[selected + 1] || p <= cdf[selected]) {
      throw new RuntimeException("bruh");
    }

    return selected;
  }

  public static void main(String[] args) {
    var ga =
        new GeneticAlgorithm(
            Samples.readInstance("NE-CESVP-2011-M-D.xml"),
            new Weights(new HardWeights(100.0, 100.0, 80.0, 90.0, 60.0), new SoftWeights(1.0, 2.0)),
            100, 1,
            50_000, 0.1);
    ga.run();
//    var list = List.of(6, 5, 4, 3, 2, 1);
//    System.out.println(list.stream().sorted().limit(3).toList());
  }
}
