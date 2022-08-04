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

public final class CostFunction {
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
  }

  boolean isTeacherUnavailable(int teacher, int period) {
    return isTeacherUnavailable[period][teacher];
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

  SoftCost computeSoftCost(int[][] timetable) {
    return new SoftCost(computeTeacherIdleness(timetable), computeDoubleLessonCost(timetable));
  }

  TotalCost computeTotalCost(int[][] timetable) {
    return new TotalCost(computeHardCost(timetable), computeSoftCost(timetable));
  }
}
