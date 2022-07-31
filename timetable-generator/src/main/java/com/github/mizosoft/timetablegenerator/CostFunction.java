package com.github.mizosoft.timetablegenerator;

import com.github.mizosoft.timetablegenerator.Models.Group;
import com.github.mizosoft.timetablegenerator.Models.HardCost;
import com.github.mizosoft.timetablegenerator.Models.ProblemInstance;
import com.github.mizosoft.timetablegenerator.Models.SoftCost;
import com.github.mizosoft.timetablegenerator.Models.Teacher;
import com.github.mizosoft.timetablegenerator.Models.TotalCost;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class CostFunction {
  private final ProblemInstance instance;
  private final boolean[][] isTeacherUnavailable;
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
  }

  //
  //  private int[][] teacherFreq;
  //  private int teacherClashes;
  //  private int teacherUnavailabilities;
  //  private int[][][] lessonFreq;
  //  private int dailyExceedances;
  //  private int groupIdleness;
  //  private List<List<Set<Integer>>> groupBusySlots;
  //
  //  void initialize(int[][] timetable) {
  //
  //  }
  //
  //  void update(int[][] timetable, int group, int teacher, int fromPeriod, int toPeriod) {
  //    if (--teacherFreq[teacher][fromPeriod] > 1) {
  //      teacherClashes--;
  //    }
  //    if (++teacherFreq[teacher][toPeriod] > 1) {
  //      teacherClashes++;
  //    }
  //
  //    if (isTeacherUnavailable[teacher][fromPeriod]) {
  //      teacherUnavailabilities--;
  //    }
  //    if (isTeacherUnavailable[teacher][toPeriod]) {
  //      teacherUnavailabilities++;
  //    }
  //
  //    int fromDay = fromPeriod / instance.slotCount();
  //    if (--lessonFreq[fromDay][group][teacher] >= maxDailyOccurrences[group][teacher]) {
  //      dailyExceedances--;
  //    }
  //
  //    int toDay = toPeriod / instance.slotCount();
  //    if (++lessonFreq[toDay][group][teacher] > maxDailyOccurrences[group][teacher]) {
  //      dailyExceedances++;
  //    }
  //
  //    int fromSlot = fromPeriod % instance.slotCount();
  //    int toSlot = toPeriod % instance.slotCount();
  //
  //    groupBusySlots.get(group).get(fromDay).remove(fromSlot);
  //    groupBusySlots.get(group).get(toSlot).add(toSlot);
  //
  ////    int fromSlot = fromPeriod % instance.slotCount();
  ////    if (!isAtEndOfDay(timetable, group, fromDay, fromSlot)) {
  ////      groupIdleness++;
  ////    }
  ////
  ////    int toSlot = toPeriod % instance.slotCount();
  ////    if (isAtEndOfDay(timetable, group, toDay, toSlot)) {
  ////
  ////    }
  //  }
  //
  //  private boolean isAtEndOfDay(int[][] timetable, int group, int day, int slot) {
  //    for (int s = slot + 1; s < instance.slotCount(); s++) {
  //      if (timetable[day * instance.slotCount() + s][group] != -1) {
  //        return false;
  //      }
  //    }
  //    return true;
  //  }

  HardCost computeHardCost(int[][] timetable) {
    int teacherClashes = 0;
    int teacherUnavailabilities = 0;
    for (int period = 0; period < instance.periodCount(); period++) {
      var teacherIsBusy = new boolean[instance.teacherCount()];
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

    // The sequence of slots allotted to each group in each day
    var busySlots =
        Stream.generate(
                () ->
                    Stream.<List<Integer>>generate(ArrayList::new)
                        .limit(instance.dayCount())
                        .toList())
            .limit(instance.groupCount())
            .toList();
    for (int period = 0; period < instance.periodCount(); period++) {
      int day = period / instance.slotCount();
      int slot = period % instance.slotCount();
      for (int group = 0; group < instance.groupCount(); group++) {
        if (timetable[period][group] != -1) {
          busySlots.get(group).get(day).add(slot);
        }
      }
    }

    // Calculates number of free slots between each two busy slots, or the beginning of day and the
    // first busy slot, (i.e. the number of free slots not at the end of day). This makes sure a
    // class is kept busy until the end of day.
    int groupIdleness = 0;
    for (int group = 0; group < instance.groupCount(); group++) {
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
      var occurrences = new int[instance.groupCount()][instance.teacherCount()];
      for (int slot = 0; slot < instance.slotCount(); slot++) {
        for (int group = 0; group < instance.groupCount(); group++) {
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
            .limit(instance.teacherCount())
            .toList();
    for (int period = 0; period < instance.periodCount(); period++) {
      int day = period / instance.slotCount();
      int slot = period % instance.slotCount();
      for (int group = 0; group < instance.groupCount(); group++) {
        int teacher = timetable[period][group];
        if (teacher != -1) {
          busySlots.get(teacher).get(day).add(slot);
        }
      }
    }

    int teacherIdleness = 0;
    for (int teacher = 0; teacher < instance.teacherCount(); teacher++) {
      for (int day = 0; day < instance.dayCount(); day++) {
        int last = -1; // -1 Marks beginning of day
        for (int slot : busySlots.get(teacher).get(day)) {
          // All busy slots have to be directly after each other (diff = 1)
          teacherIdleness += slot - last - 1;
          last = slot;
        }
      }
    }

    var doubleLessonCount = new int[instance.groupCount()][instance.teacherCount()];
    for (int day = 0; day < instance.dayCount(); day++) {
      var latestLessonSlot = new int[instance.groupCount()][instance.teacherCount()];
      for (var row : latestLessonSlot) {
        Arrays.fill(row, -1);
      }

      for (int slot = 0; slot < instance.slotCount(); slot++) {
        for (int group = 0; group < instance.groupCount(); group++) {
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

    var doubleLessonCount = new int[instance.teacherCount()];
    for (int day = 0; day < instance.dayCount(); day++) {
      var latestLessonSlot = new int[instance.teacherCount()];
      Arrays.fill(latestLessonSlot, -1);

      for (int slot = 0; slot < instance.slotCount(); slot++) {
        int teacher = timetable[day * instance.slotCount() + slot][group];
        if (teacher != -1) {
          int prevSlot = latestLessonSlot[teacher];
          if (prevSlot != -1 && prevSlot == slot - 1) {
            doubleLessonCount[teacher]++;
            latestLessonSlot[teacher] = -1; // Don't pair with next slot
          } else {
            latestLessonSlot[teacher] = slot;
          }
        }
      }
    }

    int doubleLessonsCost = 0;
    for (int teacher = 0; teacher < instance.teacherCount(); teacher++) {
      doubleLessonsCost +=
          Math.abs(doubleLessons[group][teacher] - doubleLessonCount[teacher]);
    }

    return new TotalCost(
        new HardCost(teacherClashes, 0, groupIdleness, teacherUnavailabilities, dailyExceedances),
        new SoftCost(teacherIdleness, doubleLessonsCost ));
  }

  TotalCost computeTotalCost(int[][] timetable) {
    return new TotalCost(computeHardCost(timetable), computeSoftCost(timetable));
  }
}
