package com.github.mizosoft.timetablegenerator;

import com.github.mizosoft.timetablegenerator.Models.Group;
import com.github.mizosoft.timetablegenerator.Models.HardCost;
import com.github.mizosoft.timetablegenerator.Models.HardWeights;
import com.github.mizosoft.timetablegenerator.Models.ProblemInstance;
import com.github.mizosoft.timetablegenerator.Models.SoftCost;
import com.github.mizosoft.timetablegenerator.Models.SoftWeights;
import com.github.mizosoft.timetablegenerator.Models.Teacher;
import com.github.mizosoft.timetablegenerator.Models.TotalCost;
import com.github.mizosoft.timetablegenerator.Models.Weights;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Solver {
  private static final int POPULATION_SIZE = 80;
  private static final int MAX_GA_ITERATIONS = 50_000;
  private final ProblemInstance instance;
  private final int periodCount;
  private final int teacherCount;
  private final int groupCount;
  private final Indexer<Teacher> teacherIndexer;
  private final Indexer<Group> groupIndexer;

  private final boolean[][] isTeacherUnavailable;
  private final int[][] maxDailyOccurrences;
  private final int[][] doubleLessons;

  private static final Weights weights = new Weights(new HardWeights(100.0, 100.0, 75.0, 90.0, 60.0), new SoftWeights(2.0, 5.0));

  Solver(ProblemInstance instance) {
    this.instance = instance;
    periodCount = instance.dayCount() * instance.slotCount();
    groupCount = instance.groupCount();
    teacherCount = instance.teacherCount();
    teacherIndexer = new HashIndexer<>(instance.teachers());
    groupIndexer = new HashIndexer<>(instance.groups());

    isTeacherUnavailable = new boolean[periodCount][instance.teacherCount()];
    for (var entry : instance.teacherUnavailabilities().entrySet()) {
      var teacher = entry.getKey();
      for (var period : entry.getValue()) {
        // Assumes day-major layout
        int periodIndex = period.day() * instance.slotCount() + period.slot();
        isTeacherUnavailable[periodIndex][teacherIndexer.indexOf(teacher)] = true;
      }
    }

    maxDailyOccurrences = new int[groupCount][teacherCount];
    for (var entry : instance.maxDailyOccurrences().entrySet()) {
      var lesson = entry.getKey();
      int group = groupIndexer.indexOf(lesson.group());
      int teacher = teacherIndexer.indexOf(lesson.teacher());
      maxDailyOccurrences[group][teacher] = entry.getValue();
    }

    doubleLessons = new int[groupCount][teacherCount];
    for (var entry : instance.doubleLessons().entrySet()) {
      var lesson = entry.getKey();
      int group = groupIndexer.indexOf(lesson.group());
      int teacher = teacherIndexer.indexOf(lesson.teacher());
      doubleLessons[group][teacher] = entry.getValue();
    }
  }


  private TotalCost computeTotalCost(int[][] timetable) {
    return new TotalCost(computeHardCost(timetable), computeSoftCost(timetable));
  }

  List<int[][]> population;

  void geneticAlgorithm() {
    var population = Stream.generate(this::generateTimetable).parallel().limit(POPULATION_SIZE).toList();

    System.out.println(
        population.stream()
            .map(this::computeTotalCost)
            .sorted(Comparator.comparingInt(cost -> cost.total(weights)))
            .limit(10)
            .map(Object::toString)
            .collect(Collectors.joining(", ")));

    population = population.parallelStream().map(table -> simulatedAnnealing1(table, 10_000)).toList();
//
//    System.out.println(
//        population.stream()
//            .map(this::computeTotalCost)
//            .sorted(Comparator.comparingInt(TotalCost::total))
//            .limit(10)
//            .map(Object::toString)
//            .collect(Collectors.joining(", ")));
//
//    System.out.println(
//        population.stream()
//            .map(this::computeHardCost)
//            .sorted(Comparator.comparingInt(HardCost::total))
//            .limit(10)
//            .map(Object::toString)
//            .collect(Collectors.joining(", ")));

    for (int i = 0; i < MAX_GA_ITERATIONS; i++) {
      int ceiling = 0;
      var costs = new ArrayList<TotalCost>(POPULATION_SIZE);
      for (var individual : population) {
        var cost = computeTotalCost(individual);
        costs.add(cost);
        ceiling = Math.max(ceiling, cost.total(weights));
      }

      // Don't allow zero fitness
      ceiling += Math.max(1, (int) Math.ceil(ceiling * 0.1));

      double sum = 0.0;
      var fitness = new int[POPULATION_SIZE];
      for (int j = 0; j < POPULATION_SIZE; j++) {
        var cost = costs.get(j);
        fitness[j] = ceiling - cost.total(weights);
        sum += fitness[j];
      }

      if (sum == 0) {
        throw new RuntimeException("bruh");
      }

      var cdf = new double[POPULATION_SIZE + 1];
      for (int j = 1; j < cdf.length; j++) {
        cdf[j] = cdf[j - 1] + fitness[j - 1] / sum;
      }

      //      var matingPool = new ArrayList<int[][]>();
      //      for (int j = 0; j < POPULATION_SIZE / 3; j++) {
      //        matingPool.add(population.get(rouletteWheel(cdf)));
      //      }

      var newPopulation = new ArrayList<int[][]>();

      newPopulation.add(population.stream()
              .min(Comparator.comparing(
                  this::computeTotalCost, Comparator.comparingInt(cost -> cost.total(weights))))
          .orElseThrow());

      boolean strategy = ThreadLocalRandom.current().nextDouble() < 0.6;
//      boolean strategy = true;
      while (newPopulation.size() < POPULATION_SIZE) {
        newPopulation.add(generateOffspring(population, cdf, strategy));
      }

      population = List.copyOf(newPopulation);

      if (this.population != null) {
        var c1 = this.population.stream().min(Comparator.comparing(
                this::computeTotalCost, Comparator.comparingInt(cost -> cost.total(weights))))
            .map(this::computeTotalCost)
            .orElseThrow();
        var c2 = population.stream().min(Comparator.comparing(
                this::computeTotalCost, Comparator.comparingInt(cost -> cost.total(weights))))
            .map(this::computeTotalCost)
            .orElseThrow();

        if (c1.hardCost().total() == 0 && c1.total() == c2.total()) {
          break;
        }
      }

      this.population = population;

//      if (i % 1000 == 0) {
//        population = population.parallelStream().map(this::simulatedAnnealing1).toList();
//      }
//
//      for (var table : population) {
//        if (computeHardCost(table).total() == 0) {}
//      }

//      mutationRate *= 1.0001;
      mutationRate += 0.01 * i / MAX_GA_ITERATIONS;
      if ((i % 500) == 0) {
        System.out.println(mutationRate);
        System.out.println(i);
        System.out.println(
            population.stream()
                .map(this::computeTotalCost)
                .sorted(Comparator.comparingInt(cost -> cost.total(weights)))
                .limit(10)
                .map(Object::toString)
                .collect(Collectors.joining(", ")));
      }
    }
  }

  private int[][] generateTimetable2() {
    // urgency
    throw new UnsupportedOperationException("TODO");
  }

  private int[][] generateTimetable() {
    var timetable = new int[periodCount][groupCount];
    for (var row : timetable) {
      Arrays.fill(row, -1);
    }

    var freeSlots = new ArrayList<Integer>();
    for (var lesson : instance.lessons()) {
      int group = groupIndexer.indexOf(lesson.group());
      int lessonCount = instance.weeklyOccurrences().get(lesson);
      for (int i = 0; i < lessonCount; i++) {
        for (int period = 0; period < periodCount; period++) {
          if (timetable[period][group] == -1) {
            freeSlots.add(period);
          }
        }

        if (freeSlots.isEmpty()) {
          throw new IllegalStateException("bruh");
        }

        timetable[freeSlots.get(ThreadLocalRandom.current().nextInt(freeSlots.size()))][group] =
            teacherIndexer.indexOf(lesson.teacher());
      }
    }
    return timetable;
  }

  private HardCost computeHardCost(int[][] timetable) {
    int teacherClashes = 0;
    int teacherUnavailabilities = 0;
    for (int period = 0; period < periodCount; period++) {
      var teacherIsBusy = new boolean[teacherCount];
      for (int group = 0; group < groupCount; group++) {
        int teacher = timetable[period][group];
        if (teacher != -1) {
          if (teacherIsBusy[teacher]) {
            teacherClashes++;
          }
          teacherIsBusy[teacher] = true;
          if (isTeacherUnavailable[period][teacher]) {
            teacherUnavailabilities++;
          }
        }
      }
    }

    // The sequence of slots allotted to each group in each day
    var busySlots =
        Stream.generate(
                () ->
                    Stream.<List<Integer>>generate(ArrayList::new)
                        .limit(instance.dayCount())
                        .toList())
            .limit(instance.groupCount())
            .toList();
    for (int period = 0; period < periodCount; period++) {
      int day = period / instance.slotCount();
      int slot = period % instance.slotCount();
      for (int group = 0; group < groupCount; group++) {
        if (timetable[period][group] != -1) {
          busySlots.get(group).get(day).add(slot);
        }
      }
    }

    // Calculates number of free slots between each two busy slots, or the beginning of day and the
    // first busy slot, (i.e. the number of free slots not at the end of day). This makes sure a
    // class is kept busy until the end of day.
    int groupIdleness = 0;
    for (int group = 0; group < groupCount; group++) {
      for (int day = 0; day < instance.dayCount(); day++) {
        int last = -1; // -1 Marks beginning of day
        for (int slot : busySlots.get(group).get(day)) {
          // All busy slots have to be directly after each other (diff = 1)
          groupIdleness += slot - last - 1;
          last = slot;
        }
      }
    }

    int dailyExceedances = 0;
    for (int day = 0; day < instance.dayCount(); day++) {
      var occurrences = new int[groupCount][teacherCount];
      for (int slot = 0; slot < instance.slotCount(); slot++) {
        for (int group = 0; group < groupCount; group++) {
          int teacher = timetable[day * instance.slotCount() + slot][group];
          if (teacher != -1) {
            if (++occurrences[group][teacher] > maxDailyOccurrences[group][teacher]) {
              dailyExceedances++;
            }
          }
        }
      }
    }

    return new HardCost(
        teacherClashes, 0, groupIdleness, teacherUnavailabilities, dailyExceedances);
  }

  private SoftCost computeSoftCost(int[][] timetable) {
    // The sequence of slots allotted to each teacher in each day
    var busySlots =
        Stream.generate(
                () ->
                    Stream.<List<Integer>>generate(ArrayList::new)
                        .limit(instance.dayCount())
                        .toList())
            .limit(teacherCount)
            .toList();
    for (int period = 0; period < periodCount; period++) {
      int day = period / instance.slotCount();
      int slot = period % instance.slotCount();
      for (int group = 0; group < groupCount; group++) {
        int teacher = timetable[period][group];
        if (teacher != -1) {
          busySlots.get(teacher).get(day).add(slot);
        }
      }
    }

    int teacherIdleness = 0;
    for (int teacher = 0; teacher < teacherCount; teacher++) {
      for (int day = 0; day < instance.dayCount(); day++) {
        int last = -1; // -1 Marks beginning of day
        for (int slot : busySlots.get(teacher).get(day)) {
          // All busy slots have to be directly after each other (diff = 1)
          teacherIdleness += slot - last - 1;
          last = slot;
        }
      }
    }

    var doubleLessonCount = new int[groupCount][teacherCount];
    for (int day = 0; day < instance.dayCount(); day++) {
      var latestLessonSlot = new int[groupCount][teacherCount];
      for (var row : latestLessonSlot) {
        Arrays.fill(row, -1);
      }

      for (int slot = 0; slot < instance.slotCount(); slot++) {
        for (int group = 0; group < groupCount; group++) {
          int teacher = timetable[day * instance.slotCount() + slot][group];
          if (teacher != -1) {
            int prevSlot = latestLessonSlot[group][teacher];
            if (prevSlot != -1 && prevSlot == slot - 1) {
              doubleLessonCount[group][teacher]++;
              latestLessonSlot[group][teacher] = -1; // Don't pair with next slot
            } else {
              latestLessonSlot[group][teacher] = slot;
            }
          }
        }
      }
    }

    int doubleLessonsCost = 0;
    for (int group = 0; group < instance.groupCount(); group++) {
      for (int teacher = 0; teacher < instance.teacherCount(); teacher++) {
          doubleLessonsCost +=
              Math.abs(doubleLessons[group][teacher] - doubleLessonCount[group][teacher]);
      }
    }

    return new SoftCost(teacherIdleness, doubleLessonsCost);
  }

  private int[][] generateOffspring(List<int[][]> population, double[] cdf, boolean strategy) {
    var firstParent = population.get(rouletteWheel(cdf));
    var secondParent = population.get(rouletteWheel(cdf));

    var pickFirst = new boolean[groupIndexer.size()];
    if (strategy) {
      for (int group = 0; group < groupCount; group++) {
        int firstCost =  computeTotalCostForGroup(firstParent, group).total(weights);
        int secondCost =  computeTotalCostForGroup(secondParent, group).total(weights);
        pickFirst[group] = firstCost < secondCost || (firstCost == secondCost
            && ThreadLocalRandom.current().nextDouble() < 0.5);
      }
    } else {
      for (int group = 0; group < groupCount; group++) {
        pickFirst[group] = ThreadLocalRandom.current().nextDouble() < 0.5;
      }
    }

    var offspring = new int[periodCount][groupIndexer.size()];
    for (int period = 0; period < periodCount; period++) {
      for (int group = 0; group < groupIndexer.size(); group++) {
        if (pickFirst[group]) {
          offspring[period][group] = firstParent[period][group];
        } else {
          offspring[period][group] = secondParent[period][group];
        }
      }
    }

    for (int group = 0; group < groupIndexer.size(); group++) {
      if (ThreadLocalRandom.current().nextDouble() < mutationRate) {
        int fromPeriod = ThreadLocalRandom.current().nextInt(periodCount);
        int toPeriod = ThreadLocalRandom.current().nextInt(periodCount);

        int temp = offspring[fromPeriod][group];
        offspring[fromPeriod][group] = offspring[toPeriod][group];
        offspring[toPeriod][group] = temp;
      }
    }

//        simulatedAnnealing1(offspring);

    return offspring;
  }

  private TotalCost computeTotalCostForGroup(int[][] timetable, int group) {
    int teacherClashes = 0;
    for (int period = 0; period < periodCount; period++) {
      for (int otherGroup = 0; otherGroup < groupCount; otherGroup++) {
        if (otherGroup != group && timetable[period][otherGroup] == timetable[period][group]
            && timetable[period][group] != -1) {
          teacherClashes++;
        }
      }
    }

    int teacherUnavailabilities = 0;
    for (int period = 0; period < periodCount; period++) {
      int teacher = timetable[period][group];
      if (teacher != -1 && isTeacherUnavailable[period][teacher]) {
        teacherUnavailabilities++;
      }
    }

    // The sequence of slots allotted to this group in each day
    var busySlots =
        Stream.<List<Integer>>generate(ArrayList::new)
            .limit(instance.dayCount())
            .toList();
    for (int period = 0; period < periodCount; period++) {
      int day = period / instance.slotCount();
      int slot = period % instance.slotCount();
      if (timetable[period][group] != -1) {
        busySlots.get(day).add(slot);
      }
    }

    int groupIdleness = 0;
    for (int day = 0; day < instance.dayCount(); day++) {
      int last = -1; // -1 Marks beginning of day
      for (int slot : busySlots.get(day)) {
        // All busy slots have to be directly after each other (diff = 1)
        groupIdleness += (slot - last - 1);
        last = slot;
      }
    }

    int dailyExceedances = 0;
    for (int day = 0; day < instance.dayCount(); day++) {
      var occurrences = new int[teacherCount];
      for (int slot = 0; slot < instance.slotCount(); slot++) {
        int teacher = timetable[day * instance.slotCount() + slot][group];
        if (teacher != -1) {
          if (++occurrences[teacher] > maxDailyOccurrences[group][teacher]) {
            dailyExceedances++;
          }
        }
      }
    }

    // The sequence of slots allotted to each teacher in each day
    var teacherBusiness =
        Stream.generate(
                () ->
                    Stream.<List<Integer>>generate(ArrayList::new)
                        .limit(instance.dayCount())
                        .toList())
            .limit(teacherCount)
            .toList();
    for (int period = 0; period < periodCount; period++) {
      int day = period / instance.slotCount();
      int slot = period % instance.slotCount();
      int teacher = timetable[period][group];
      if (teacher != -1) {
        teacherBusiness.get(teacher).get(day).add(slot);
      }
    }

    int teacherIdleness = 0;
    for (int teacher = 0; teacher < teacherCount; teacher++) {
      for (int day = 0; day < instance.dayCount(); day++) {
        int last = -1; // -1 Marks beginning of day
        for (int slot : teacherBusiness.get(teacher).get(day)) {
          // All busy slots have to be directly after each other (diff = 1)
          teacherIdleness += (slot - last - 1);
          last = slot;
        }
      }
    }

//    var doubleLessonCount = new int[teacherCount];
//    for (int day = 0; day < instance.dayCount(); day++) {
//      var latestLessonSlot = new int[teacherCount];
//      Arrays.fill(latestLessonSlot, -1);
//
//      for (int slot = 0; slot < instance.slotCount(); slot++) {
//        int teacher = timetable[day * instance.slotCount() + slot][group];
//        if (teacher != -1) {
//          int prevSlot = latestLessonSlot[teacher];
//          if (prevSlot == slot - 1) {
//            doubleLessonCount[teacher]++;
//            latestLessonSlot[teacher] = -1; // Don't pair with next slot
//          } else {
//            latestLessonSlot[teacher] = slot;
//          }
//        }
//      }
//    }
//
//    int doubleLessonsCost = 0;
//    for (int teacher = 0; teacher < instance.teacherCount(); teacher++) {
//      doubleLessonsCost +=
//          Math.abs(doubleLessons[group][teacher] - doubleLessonCount[teacher]);
//    }

    return new TotalCost(
        new HardCost(teacherClashes, 0, groupIdleness, teacherUnavailabilities, dailyExceedances), new SoftCost(teacherIdleness, 0));
  }

  private double mutationRate = 0.05;

  private int rouletteWheel(double[] cdf) {
    double p = ThreadLocalRandom.current().nextDouble();

    // TODO we can implement this with binary search.
    int selected = 0;
    for (int j = 1; j < POPULATION_SIZE && p > cdf[j]; j++) {
      selected = j;
    }

    if (p > cdf[selected + 1] || p <= cdf[selected]) {
      throw new RuntimeException("bruh");
    }

    return selected;
  }

  int[][] simulatedAnnealing1(int[][] table, int iter) {
    //    var tracker = new CostTracker(individual);
    //    System.out.println("Initial cost: " + tracker.cost());

          var mutatedTable = new int[periodCount][groupIndexer.size()];
          for (int period = 0; period < periodCount; period++) {
            System.arraycopy(table[period], 0, mutatedTable[period], 0, groupIndexer.size());
          }

//    int iterations = 200_000;
    int iterations = iter;
    double temperature = 100;
    for (int i = 0; i < iterations; i++) {
      //      int fromPeriod = rnd.nextInt(periodCount);
      //      int toPeriod = rnd.nextInt(periodCount);
      //      if (fromPeriod == toPeriod) {
      //        //        System.out.println("from == to!");
      //        continue;
      //      }
      //      if (individual.get(fromPeriod).isEmpty()) {
      //        //        System.out.println("from empty!");
      //        continue;
      //      }

      var cost = computeTotalCost(mutatedTable);


      record Mutation(int group, int fromPeriod, int toPeriod) {}
      var mutations = new ArrayList<Mutation>();
      for (int group = 0; group < groupIndexer.size(); group++) {
        if (ThreadLocalRandom.current().nextDouble() < 0.1) {
          int fromPeriod = ThreadLocalRandom.current().nextInt(periodCount);
          int toPeriod = ThreadLocalRandom.current().nextInt(periodCount);

          int temp = mutatedTable[fromPeriod][group];
          mutatedTable[fromPeriod][group] = mutatedTable[toPeriod][group];
          mutatedTable[toPeriod][group] = temp;

          mutations.add(new Mutation(group, fromPeriod, toPeriod));
        }
      }

//      int group = ThreadLocalRandom.current().nextInt(groupIndexer.size());
//      int fromPeriod = ThreadLocalRandom.current().nextInt(periodCount);
//      int toPeriod = ThreadLocalRandom.current().nextInt(periodCount);
//      int temp = table[fromPeriod][group];
//      table[fromPeriod][group] = table[toPeriod][group];
//      table[toPeriod][group] = temp;

      var updatedCost = computeTotalCost(mutatedTable);

      //      var updatedCost = tracker.cost();

      //      System.out.println("cost: " + cost.total());
      //      System.out.println("newCost: " + updatedCost.total());

      //      temperature = initialTemp / (1 + i);
      int delta = updatedCost.total(weights) - cost.total(weights);
      double p = Math.exp(-delta / (temperature / (1 + i)));
      //      double p = Math.exp(-delta / temperature);
      //      temperature *= 0.9;
      if (delta < 0
          || (delta != 0 && ThreadLocalRandom.current().nextDouble() < p)
          || (delta == 0 && ThreadLocalRandom.current().nextDouble() < 0.2)) {
        //        if (delta < 0) {
        //          System.out.println("Better cost accepted");
        //        } else if (delta > 0) {
        //          System.out.println("Worse cost accepted: " + Math.exp(-delta / (temperature / (1
        // + i))));
        //        }
        // TODO this remove can probably be optimized (consider using indices instead of actual
        //      requirements)
        //        table.get(fromPeriod).remove((Object) candidate);
        //        table.get(toPeriod).add(candidate);
      } else {
        // Reverse the mutations
        for (var mut : mutations) {
          int temp = mutatedTable[mut.toPeriod()][mut.group()];
          mutatedTable[mut.toPeriod()][mut.group()] = mutatedTable[mut.fromPeriod()][mut.group()];
          mutatedTable[mut.fromPeriod()][mut.group()] = temp;
        }
        //        int temp1 = table[fromPeriod][group];
        //        table[fromPeriod][group] = table[toPeriod][group];
        //        table[toPeriod][group] = temp1;
        // Reverse cost update
        //        tracker.update(toPeriod, fromPeriod, candidate);
        //        if (!cost.equals(tracker.cost())) {
        //          System.out.println("a7a");
        //        }
      }
      //      temperature = 0.95 * temperature;

    }
    return mutatedTable;
  }

//  private final class CostTracker {
//    private int[][] teacherFreq;
//
//    private int teacherUnavailabilities;
//
//  }

  void run() {
    geneticAlgorithm();
    var mn = population.stream().min(
        Comparator.comparing(this::computeTotalCost, Comparator.comparingInt(cost -> cost.total(weights))))
        .orElseThrow();

//    var mn = generateTimetable();
    System.out.println(computeTotalCost(mn));
    mn = simulatedAnnealing1(mn, 100_000);
    System.out.println(computeTotalCost(mn));
  }

  public static void main(String[] args){
    var solver = new Solver(Samples.readInstance("NE-CESVP-2011-M-D.xml"));
    solver.run();
  }
}
