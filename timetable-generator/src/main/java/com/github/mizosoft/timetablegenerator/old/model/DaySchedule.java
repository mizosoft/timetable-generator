package com.github.mizosoft.timetablegenerator.old.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import com.github.mizosoft.timetablegenerator.old.OverlappingRangeException;
import com.github.mizosoft.timetablegenerator.old.Utils;
import static com.github.mizosoft.timetablegenerator.old.Utils.Pair;

public class DaySchedule {
  private final Map<TimeRange, Pair<Course, CourseAttendable>> schedule;
  private final List<Pair<TimeRange, Pair<Course, CourseAttendable>>> scheduleList;

  private DaySchedule(Map<TimeRange, Pair<Course, CourseAttendable>> schedule) {
    this.schedule = schedule;
    
    var list = new ArrayList<Pair<TimeRange, Pair<Course, CourseAttendable>>>();
    for (var entry : schedule.entrySet()) {
        list.add(Utils.pair(entry.getKey(), entry.getValue()));
    }
    scheduleList = List.copyOf(list);
  }

  public Map<TimeRange, Pair<Course, CourseAttendable>> getSchedule() {
    return schedule;
  }
  
  public List<Pair<TimeRange, Pair<Course, CourseAttendable>>> getScheduleList() {
    return scheduleList;
  }
  
  public int size() {
      return scheduleList.size();
  }

  @Override
  public String toString() {
    return "DaySchedule{" +
        "schedule=" + schedule +
        '}';
  }

  static final class Builder {
    private final SortedMap<TimeRange, Pair<Course, CourseAttendable>> schedule = new TreeMap<>();

    Builder() {}

    void add(TimeRange range, Course course, CourseAttendable attendable) {
      checkOverlapping(range);
      schedule.put(range, Utils.pair(course, attendable));
    }

    void checkOverlapping(TimeRange newRange) {
      schedule.keySet().stream()
              .filter(newRange::overlapsWith)
              .findAny()
              .ifPresent(r -> { throw new OverlappingRangeException(r); });
    }

    TimeRange peekTail() {
      try {
        return schedule.lastKey();
      } catch (NoSuchElementException ignored) {
        return null;
      }
    }

    DaySchedule build() {
      return new DaySchedule(Collections.unmodifiableMap(new TreeMap<>(schedule)));
    }
  }
}
