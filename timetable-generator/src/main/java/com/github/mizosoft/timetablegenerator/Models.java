package com.github.mizosoft.timetablegenerator;

import java.util.Map;
import java.util.Set;

public class Models {
  record Teacher(String id, String name) {}

  record Course(String id, String name) {}

  record Lesson(Course course, Teacher teacher, Group group) {}

  record Group(String id, String name) {}

  record Period(int day, int slot) {}

  record HardCost(int teacherClashes, int groupClashes, int groupIdleness, int teacherUnavailabilities, int dailyExceedences) {
//    HardCost(int teacherClashes, int groupClashes, int groupIdleness, int teacherUnavailabilities, int maxDailyOccurrences) {
//      this(teacherClashes + groupClashes + groupIdleness + teacherUnavailabilities + maxDailyOccurrences, teacherClashes, groupClashes, groupIdleness, teacherUnavailabilities, maxDailyOccurrences);
//    }

    public int total() {
      return teacherClashes + groupClashes + groupIdleness + teacherUnavailabilities + dailyExceedences;
    }
  }

  record SoftCost( int teacherIdleness, int doubleLessons) {
    public int total() {
      return doubleLessons + teacherIdleness;
    }
  }

  record ProblemInstance(
      int dayCount,
      int slotCount,
      int teacherCount,
      int groupCount,
      Set<Teacher> teachers,
      Set<Group> groups,
      Set<Lesson> lessons,
      Map<Lesson, Integer> weeklyOccurrences,
      Map<Lesson, Integer> maxDailyOccurrences,
      Map<Teacher, Set<Period>> teacherUnavailabilities,
      Map<Lesson, Integer> doubleLessons) {}
}
