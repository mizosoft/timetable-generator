package com.github.mizosoft.timetablegenerator;

import com.github.mizosoft.timetablegenerator.Models.Group;
import com.github.mizosoft.timetablegenerator.Models.HardCost;
import com.github.mizosoft.timetablegenerator.Models.Lesson;
import com.github.mizosoft.timetablegenerator.Models.ProblemInstance;
import com.github.mizosoft.timetablegenerator.Models.SoftCost;
import com.github.mizosoft.timetablegenerator.Models.Teacher;
import com.github.mizosoft.timetablegenerator.Models.TotalCost;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

public class CostFunction {
  private final ProblemInstance instance;
  final boolean[][] isTeacherUnavailable;
  private final int[][] maxDailyOccurrences;
  private final int[][] doubleLessons;

  CostFunction(
      ProblemInstance instance, Indexer<Teacher> teacherIndexer, Indexer<Group> groupIndexer) {
    this.instance = instance;
    isTeacherUnavailable = new boolean[instance.periodCount()][instance.teacherCount()];
    for (var entry : instance.teacherUnavailabilities().entrySet()) {
      var teacher = entry.getKey();
      for (var period : entry.getValue()) {
        // Assumes day-major layout
        int periodIndex = period.day() * instance.slotCount() + period.slot();
        isTeacherUnavailable[periodIndex][teacherIndexer.indexOf(teacher)] = true;
      }
    }

    maxDailyOccurrences = new int[instance.groupCount()][instance.teacherCount()];
    for (var entry : instance.maxDailyOccurrences().entrySet()) {
      var lesson = entry.getKey();
      int group = groupIndexer.indexOf(lesson.group());
      int teacher = teacherIndexer.indexOf(lesson.teacher());
      maxDailyOccurrences[group][teacher] = entry.getValue();
    }

    doubleLessons = new int[instance.groupCount()][instance.teacherCount()];
    for (var entry : instance.doubleLessons().entrySet()) {
      var lesson = entry.getKey();
      int group = groupIndexer.indexOf(lesson.group());
      int teacher = teacherIndexer.indexOf(lesson.teacher());
      doubleLessons[group][teacher] = entry.getValue();
    }

    var lessonCountPerWeek = new int[instance.groupCount()];
    for (var lesson : instance.lessons()) {
      int weeklyOccurrences = instance.weeklyOccurrences().get(lesson);
      for (int i = 0; i < weeklyOccurrences; i++) {
        lessonCountPerWeek[groupIndexer.indexOf(lesson.group())]++;
      }
    }
  }

  private int computeIdleness(List<List<Integer>> busyPeriods) {
    int idleness = 0;
    for (var periods : busyPeriods) {
      for (int j = 0; j < periods.size(); j++) {
        int period = periods.get(j);
        int slot = period % instance.slotCount();

        int prevSlot = -1;
        if (j > 0) {
          int day = period / instance.slotCount();
          int prevPeriod = periods.get(j - 1);
          int prevDay = prevPeriod / instance.slotCount();
          if (prevDay == day) {
            prevSlot = prevPeriod % instance.slotCount();
          }
        }

        // Penalize when busy slots aren't directly after each other (diff > 1)
        idleness += (slot - prevSlot - 1);
      }
    }
    return idleness;
  }

  private int computeGroupIdleness(int[][] table) {
    var groupBusyPeriods =
        Stream.<List<Integer>>generate(ArrayList::new).limit(instance.groupCount()).toList();
    for (int period = 0; period < instance.periodCount(); period++) {
      for (int group = 0; group < instance.groupCount(); group++) {
        if (table[period][group] != -1) {
          groupBusyPeriods.get(group).add(period);
        }
      }
    }
    return computeIdleness(groupBusyPeriods);
  }

  private int computeTeacherIdleness(int[][] table) {
    var teacherBusyPeriods =
        Stream.<List<Integer>>generate(ArrayList::new).limit(instance.teacherCount()).toList();
    for (int period = 0; period < instance.periodCount(); period++) {
      for (int group = 0; group < instance.groupCount(); group++) {
        int teacher = table[period][group];
        if (teacher != -1) {
          teacherBusyPeriods.get(teacher).add(period);
        }
      }
    }
    return computeIdleness(teacherBusyPeriods);
  }

  HardCost computeHardCost(int[][] timetable) {
    int teacherClashes = 0;
    int teacherUnavailabilities = 0;
    var teacherIsBusy = new boolean[instance.teacherCount()];
    for (int period = 0; period < instance.periodCount(); period++) {
      Arrays.fill(teacherIsBusy, false);
      for (int group = 0; group < instance.groupCount(); group++) {
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

    int groupIdleness = computeGroupIdleness(timetable);

    int dailyExceedances = 0;
    var dailyOccurrences = new int[instance.groupCount()][instance.teacherCount()];
    for (int day = 0; day < instance.dayCount(); day++) {
      for (var row : dailyOccurrences) {
        Arrays.fill(row, 0);
      }
      for (int slot = 0; slot < instance.slotCount(); slot++) {
        for (int group = 0; group < instance.groupCount(); group++) {
          int teacher = timetable[day * instance.slotCount() + slot][group];
          if (teacher != -1) {
            if (++dailyOccurrences[group][teacher] > maxDailyOccurrences[group][teacher]) {
              dailyExceedances++;
            }
          }
        }
      }
    }

    return new HardCost(
        teacherClashes, 0, groupIdleness, teacherUnavailabilities, dailyExceedances);
  }

  private int computeDoubleLessonCost(int[][] table) {
    var doubleLessonCount = new int[instance.groupCount()][instance.teacherCount()];

    var latestLessonPeriod = new int[instance.groupCount()][instance.teacherCount()];
    for (var row : latestLessonPeriod) {
      Arrays.fill(row, -1);
    }

    for (int period = 0; period < instance.periodCount(); period++) {
      int day = period / instance.slotCount();
      int slot = period % instance.slotCount();
      for (int group = 0; group < instance.groupCount(); group++) {
        int teacher = table[period][group];
        if (teacher != -1) {
          int prevPeriod = latestLessonPeriod[group][teacher];
          int prevSlot = -1;
          if (prevPeriod != -1) {
            int prevDay = prevPeriod / instance.slotCount();
            if (prevDay == day) {
              prevSlot = prevPeriod % instance.slotCount();
            }
          }

          // See if slot pairs with prevSlot
          if (prevSlot != -1 && prevSlot == slot - 1) {
            doubleLessonCount[group][teacher]++;
            // Don't set latestLessonPeriod to not pair this lesson with next lessons
          } else {
            latestLessonPeriod[group][teacher] = period;
          }
        }
      }
    }

    int doubleLessonCost = 0;
    for (int group = 0; group < instance.groupCount(); group++) {
      for (int teacher = 0; teacher < instance.teacherCount(); teacher++) {
        doubleLessonCost +=
            Math.max(0, doubleLessons[group][teacher] - doubleLessonCount[group][teacher]);
      }
    }

    return doubleLessonCost;
  }

  private SoftCost computeSoftCost(int[][] timetable) {
    return new SoftCost(computeTeacherIdleness(timetable), computeDoubleLessonCost(timetable));
  }

  TotalCost computeTotalCostForGroup(int[][] timetable, int group) {
    int teacherClashes = 0;
    for (int period = 0; period < instance.periodCount(); period++) {
      for (int otherGroup = 0; otherGroup < instance.groupCount(); otherGroup++) {
        if (otherGroup != group
            && timetable[period][otherGroup] == timetable[period][group]
            && timetable[period][group] != -1) {
          teacherClashes++;
        }
      }
    }

    int teacherUnavailabilities = 0;
    for (int period = 0; period < instance.periodCount(); period++) {
      int teacher = timetable[period][group];
      if (teacher != -1 && isTeacherUnavailable[period][teacher]) {
        teacherUnavailabilities++;
      }
    }

    // The sequence of slots allotted to this group in each day
    var busySlots =
        Stream.<List<Integer>>generate(ArrayList::new).limit(instance.dayCount()).toList();
    for (int period = 0; period < instance.periodCount(); period++) {
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
      var occurrences = new int[instance.teacherCount()];
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
            .limit(instance.teacherCount())
            .toList();
    for (int period = 0; period < instance.periodCount(); period++) {
      int day = period / instance.slotCount();
      int slot = period % instance.slotCount();
      int teacher = timetable[period][group];
      if (teacher != -1) {
        teacherBusiness.get(teacher).get(day).add(slot);
      }
    }

    int teacherIdleness = 0;
    for (int teacher = 0; teacher < instance.teacherCount(); teacher++) {
      for (int day = 0; day < instance.dayCount(); day++) {
        int last = -1; // -1 Marks beginning of day
        for (int slot : teacherBusiness.get(teacher).get(day)) {
          // All busy slots have to be directly after each other (diff = 1)
          teacherIdleness += (slot - last - 1);
          last = slot;
        }
      }
    }

    int sameLessonCloseness = 0;
    var doubleLessonCount = new int[instance.teacherCount()];
    for (int day = 0; day < instance.dayCount(); day++) {
      var latestLessonSlot = new int[instance.teacherCount()];
      Arrays.fill(latestLessonSlot, -1);

      var availableForPairing = new boolean[instance.teacherCount()];

      for (int slot = 0; slot < instance.slotCount(); slot++) {
        int teacher = timetable[day * instance.slotCount() + slot][group];
        if (teacher != -1) {
          int prevSlot = latestLessonSlot[teacher];
          if (prevSlot != -1 && prevSlot == slot - 1 && availableForPairing[teacher]) {
            doubleLessonCount[teacher]++;
            availableForPairing[teacher] = false; // Don't pair with next slot
          } else {
            latestLessonSlot[teacher] = slot;
            availableForPairing[teacher] = true;
          }

          if (prevSlot != -1) {
            sameLessonCloseness += slot - prevSlot;
          }
        }
      }
    }

    int doubleLessonsCost = 0;
    for (int teacher = 0; teacher < instance.teacherCount(); teacher++) {
      doubleLessonsCost += Math.abs(doubleLessons[group][teacher] - doubleLessonCount[teacher]);
    }

    return new TotalCost(
        new HardCost(teacherClashes, 0, groupIdleness, teacherUnavailabilities, dailyExceedances),
        new SoftCost(teacherIdleness, doubleLessonsCost));
  }

  TotalCost computeTotalCost(int[][] timetable) {
    return new TotalCost(computeHardCost(timetable), computeSoftCost(timetable));
  }

  public static void main(String[] args) {
    var instance = Samples.readInstance();
    var groupIndexer = new HashIndexer<>(instance.groups());
    var teacherIndexer = new HashIndexer<>(instance.teachers());
    var gen = ThreadLocalRandom.current();
    var func = new CostFunction(instance, teacherIndexer, groupIndexer);
    func.printTable(generateInitialTable(instance, groupIndexer, teacherIndexer, gen));

    //    for (int i = 0; i < 10_000; i++) {
    //      var table = generateInitialTable(instance, groupIndexer, teacherIndexer, gen);
    //      int first = func.computeDoubleLessonCost(table);
    //      int second = func.computeSoftCost(table).doubleLessons();
    //      if (first != second) {
    //        throw new RuntimeException(first + ", " + second);
    //      }
    //    }
  }

  private static int[][] generateInitialTable(
      ProblemInstance instance,
      Indexer<Group> groupIndexer,
      Indexer<Teacher> teacherIndexer,
      RandomGenerator rnd) {
    var timetable = new int[instance.periodCount()][instance.groupCount()];

    for (var row : timetable) {
      Arrays.fill(row, -1);
    }

    for (var lesson : instance.lessons()) {
      int group = groupIndexer.indexOf(lesson.group());
      int lessonCount = instance.weeklyOccurrences().get(lesson);

      var freePeriods = new ArrayList<Integer>();
      for (int period = 0; period < instance.periodCount(); period++) {
        if (timetable[period][group] == -1) {
          freePeriods.add(period);
        }
      }

      for (int i = 0; i < lessonCount; i++) {
        if (freePeriods.isEmpty()) {
          throw new IllegalStateException("bruh");
        }

        int k = rnd.nextInt(freePeriods.size());
        if (timetable[freePeriods.get(k)][group] != -1) {
          throw new RuntimeException("bruh");
        }
        timetable[freePeriods.get(k)][group] = teacherIndexer.indexOf(lesson.teacher());
        freePeriods.remove(k);
      }
    }
    return timetable;
  }

  private void printTable(int[][] table) {

    int i = 0;
    var lessons = new HashMap<Lesson, Integer>();
    for (var lesson : instance.lessons()) {
      var r = lessons.put(lesson, ++i);
      if (r != null) {
        throw new RuntimeException("bruh");
      }
    }

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
          int teacher = table[day * instance.slotCount() + slot][group];
          if (teacher == -1) {
            System.out.printf("%4s", "X");
          } else {
            System.out.printf("%4d", teacher);
          }
        }
        System.out.println();
      }
    }
  }
}
