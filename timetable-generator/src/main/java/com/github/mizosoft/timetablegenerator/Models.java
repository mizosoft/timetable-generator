package com.github.mizosoft.timetablegenerator;

import java.util.Map;
import java.util.Set;

public class Models {
  private Models() {}

  record Teacher(String id, String name) {}

  record Lesson(Teacher teacher, Group group) {}

  record Group(String id, String name) {}

  record Period(int day, int slot) {}

  record HardCost(int teacherClashes, int groupClashes, int groupIdleness, int teacherUnavailabilities, int dailyExceedences) {

    public int total(HardWeights weights) {
      return (int) (teacherClashes * weights.teacherClashes()
          + groupClashes * weights.groupClashes()
          + groupIdleness * weights.groupIdleness()
          + teacherUnavailabilities * weights.teacherUnavailabilities()
          + dailyExceedences * weights.dailyExceedences());
    }
  }

  record SoftCost(int teacherIdleness, int doubleLessons) {

    public int total(SoftWeights weights) {
      return (int) (doubleLessons * weights.doubleLessons()
          + teacherIdleness * weights.teacherIdleness());
    }
  }

  record TotalCost(HardCost hardCost, SoftCost softCost) {
    public int total(Weights weights) {
      return hardCost.total(weights.hardWeights()) + softCost.total(weights.softWeights());
    }
  }

  record HardWeights(double teacherClashes, double groupClashes, double groupIdleness, double teacherUnavailabilities, double dailyExceedences) {

  }
  record SoftWeights(double teacherIdleness, double doubleLessons) {

  }
  record Weights(HardWeights hardWeights, SoftWeights softWeights) {

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
      Map<Lesson, Integer> doubleLessons) {
    public int periodCount() {
      return dayCount * slotCount;
    }
  }
}
