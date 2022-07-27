package com.github.mizosoft.timetablegenerator;

import com.github.mizosoft.timetablegenerator.Models.Group;
import com.github.mizosoft.timetablegenerator.Models.HardCost;
import com.github.mizosoft.timetablegenerator.Models.ProblemInstance;
import com.github.mizosoft.timetablegenerator.Models.SoftCost;
import com.github.mizosoft.timetablegenerator.Models.Teacher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Solver {
  private static final int POPULATION_SIZE = 80;
  private static final int GA_ITERATIONS = 20_000;
  private final ProblemInstance instance;
  private final int periodCount;
  private final int teacherCount;
  private final int groupCount;
  private final Indexer<Teacher> teacherIndexer;
  private final Indexer<Group> groupIndexer;

  private final boolean[][] isTeacherUnavailable;
  private final int[][] maxDailyOccurrences;
  private final int[][] doubleLessons;

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
      int groupIndex = groupIndexer.indexOf(lesson.group());
      int teacherIndex = teacherIndexer.indexOf(lesson.teacher());
      maxDailyOccurrences[groupIndex][teacherIndex] = entry.getValue();
    }

    doubleLessons = new int[groupCount][teacherCount];
    for (var entry : instance.doubleLessons().entrySet()) {
      var lesson = entry.getKey();
      int groupIndex = groupIndexer.indexOf(lesson.group());
      int teacherIndex = teacherIndexer.indexOf(lesson.teacher());
      doubleLessons[groupIndex][teacherIndex] = entry.getValue();
    }
  }

  void geneticAlgorithm() {
    var population = Stream.generate(this::generateTimetable)
        .map(this::simulatedAnnealing1).limit(POPULATION_SIZE).toList();

    System.out.println(
        population.stream()
            .map(this::computeHardCost)
            .sorted(Comparator.comparingInt(HardCost::total))
            .limit(10)
            .map(Object::toString)
            .collect(Collectors.joining(", ")));

    for (int i = 0; i < GA_ITERATIONS; i++) {
      int ceiling = 0;
      var costs = new ArrayList<HardCost>(POPULATION_SIZE);
      for (var individual : population) {
        var cost = computeHardCost(individual);
        costs.add(cost);
        ceiling = Math.max(ceiling, cost.total());
      }

      // Don't allow zero fitness
      ceiling += Math.max(1, (int) Math.ceil(ceiling * 0.1));

      double sum = 0.0;
      var fitness = new int[POPULATION_SIZE];
      for (int j = 0; j < POPULATION_SIZE; j++) {
        var cost = costs.get(j);
        fitness[j] = ceiling - cost.total();
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

      var prevPopulation = population;
      population =
          Stream.generate(() -> generateOffspring(prevPopulation, cdf))
              .limit(POPULATION_SIZE)
              .toList();

      for (var table : population) {
        if (computeHardCost(table).total() == 0) {}
      }

      mutationRate *= 1.0001;
      if ((i % 500) == 0) {
        System.out.println(mutationRate);
        System.out.println(i);
        System.out.println(
            population.stream()
                .map(this::computeHardCost)
                .sorted(Comparator.comparingInt(HardCost::total))
                .limit(10)
                .map(Object::toString)
                .collect(Collectors.joining(", ")));
      }
    }
  }

  private int[][] generateTimetable() {
    var timetable = new int[periodCount][groupIndexer.size()];
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
      var teacherIsBusy = new boolean[teacherIndexer.size()];
      for (int group = 0; group < groupIndexer.size(); group++) {
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
    var business =
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
          business.get(group).get(day).add(slot);
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
        for (int slot : business.get(group).get(day)) {
          if (slot - last != 1) { // All busy slots have to be directly after each other (diff = 1)
            groupIdleness += (slot - last);
          }
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
    var business =
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
          business.get(teacher).get(day).add(slot);
        }
      }
    }

    int teacherIdleness = 0;
    for (int group = 0; group < groupCount; group++) {
      for (int day = 0; day < instance.dayCount(); day++) {
        int last = -1; // -1 Marks beginning of day
        for (int slot : business.get(group).get(day)) {
          if (slot - last != 1) { // All busy slots have to be directly after each other (diff = 1)
            teacherIdleness += (slot - last);
          }
          last = slot;
        }
      }
    }

    var dailyDoubleLessons = new int[instance.dayCount()][groupCount][teacherCount];
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
            if (prevSlot == slot - 1) {
              dailyDoubleLessons[day][group][teacher]++;
              latestLessonSlot[group][teacher] = -1; // Don't pair with next slot
            } else {
              latestLessonSlot[group][teacher] = slot;
            }
          }
        }
      }
    }

    int doubleLessonsCost = 0;
    for (int day = 0; day < instance.dayCount(); day++) {
      for (int group = 0; group < instance.groupCount(); group++) {
        for (int teacher = 0; teacher < instance.groupCount(); teacher++) {
            doubleLessonsCost +=
                Math.abs(doubleLessons[group][teacher] - dailyDoubleLessons[day][group][teacher]);
        }
      }
    }

    return new SoftCost(teacherIdleness, doubleLessonsCost);
  }

  private int[][] generateOffspring(List<int[][]> population, double[] cdf) {
    var firstParent = population.get(rouletteWheel(cdf));
    var secondParent = population.get(rouletteWheel(cdf));

    var pickFirst = new boolean[groupIndexer.size()];
    for (int group = 0; group < groupIndexer.size(); group++) {
      pickFirst[group] = ThreadLocalRandom.current().nextDouble() < 0.5;
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

  private double mutationRate = 0.005;

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

  int[][] simulatedAnnealing1(int[][] table) {
    //    var tracker = new CostTracker(individual);
    //    System.out.println("Initial cost: " + tracker.cost());

          var mutatedTable = new int[periodCount][groupIndexer.size()];
          for (int period = 0; period < periodCount; period++) {
            System.arraycopy(table[period], 0, mutatedTable[period], 0, groupIndexer.size());
          }

    int iterations = 50_000;
    double temperature = 50;
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

      var cost = computeHardCost(mutatedTable);


      record Mutation(int group, int fromPeriod, int toPeriod) {}
      var mutations = new ArrayList<Mutation>();
      for (int group = 0; group < groupIndexer.size(); group++) {
        if (ThreadLocalRandom.current().nextDouble() < 0.01) {
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

      var updatedCost = computeHardCost(mutatedTable);

      //      var updatedCost = tracker.cost();

      //      System.out.println("cost: " + cost.total());
      //      System.out.println("newCost: " + updatedCost.total());

      //      temperature = initialTemp / (1 + i);
      int delta = updatedCost.total() - cost.total();
      double p = Math.exp(-delta / (temperature / (1 + i)));
      //      double p = Math.exp(-delta / temperature);
      //      temperature *= 0.9;
      if (delta < 0
          || (delta != 0 && ThreadLocalRandom.current().nextDouble() < p)
          || (delta == 0 && ThreadLocalRandom.current().nextDouble() < 0.01)) {
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
}
