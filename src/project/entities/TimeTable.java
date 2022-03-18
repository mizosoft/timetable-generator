package project.entities;

import static project.Utils.modInc;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import project.entities.DaySchedule.Builder;

public final class TimeTable {public static final DayOfWeek[] STUDY_DAYS =
      new DayOfWeek[] {
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
      };

  public static final int STUDY_DAY_BEGIN = 8;
  public static final int STUDY_DAY_END = 20;

  private final Map<DayOfWeek, DaySchedule> table;

  private TimeTable(Map<DayOfWeek, DaySchedule> table) {
    this.table = table;
  }

  public Map<DayOfWeek, DaySchedule> getTable() {
    return table;
  }

  @Override
  public String toString() {
    return "TimeTable{" +
        "table=" + table +
        '}';
  }

  public static TimeTable create(List<Course> courses) {
    var courseMap = new HashMap<Course, List<CourseAttendable>>();
    for (var course : courses) {
      courseMap.put(course, course.getAttendables());
    }

    var table = new EnumMap<DayOfWeek, Builder>(DayOfWeek.class);
    int index = 0;
    for (var entry : courseMap.entrySet()) {
      var course = entry.getKey();
      var attendables = entry.getValue();
      for (var attendable : attendables) {
        var hours = (int) attendable.getDuration().toHours();
        var scheduleBuilder = dayWithAvailableHours(table, hours, index);

        if (scheduleBuilder == null) {
          throw new IllegalStateException("bruh");
        }

        var tail = scheduleBuilder.peekTail();
        var nextRange =
            tail != null ? tail.following(hours) : TimeRange.ofDuration(STUDY_DAY_BEGIN, hours);
        scheduleBuilder.add(nextRange, course, attendable);

        index = modInc(index, STUDY_DAYS.length);
      }
    }

    return new TimeTable(
        table.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Entry::getKey, e -> e.getValue().build())));
  }

  static DaySchedule.Builder dayWithAvailableHours(
      Map<DayOfWeek, DaySchedule.Builder> map, int hours, int fromDay) {
    for (int i = fromDay, j = 0; j < STUDY_DAYS.length; i = modInc(i, STUDY_DAYS.length), j++) {
      var day = STUDY_DAYS[i];
      var builder = map.computeIfAbsent(day, __ -> new DaySchedule.Builder());
      var tail = builder.peekTail();
      if (tail == null || STUDY_DAY_END - tail.getToHour() >= hours) {
        return builder;
      }
    }

    return null;
  }
}
