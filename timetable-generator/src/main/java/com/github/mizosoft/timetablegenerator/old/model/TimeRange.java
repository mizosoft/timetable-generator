package com.github.mizosoft.timetablegenerator.old.model;

import java.util.Comparator;
import java.util.Objects;

public class TimeRange implements Comparable<TimeRange> {
  private static final Comparator<TimeRange> COMPARATOR =
      Comparator.comparingInt(TimeRange::getFromHour).thenComparing(TimeRange::getToHour);

  private final int fromHour;
  private final int toHour;

  private TimeRange(int fromHour, int toHour) {
    this.fromHour = fromHour;
    this.toHour = toHour;
  }

  public int getFromHour() {
    return fromHour;
  }

  public int getToHour() {
    return toHour;
  }

  @Override
  public int compareTo(TimeRange o) {
    return COMPARATOR.compare(this, o);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TimeRange)) {
      return false;
    }
    TimeRange timeRange = (TimeRange) o;
    return fromHour == timeRange.fromHour && toHour == timeRange.toHour;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromHour, toHour);
  }

  @Override
  public String toString() {
    return "[" + fromHour + ", " + toHour + "]";
  }

  boolean overlapsWith(TimeRange other) {
    int c = compareTo(other);
    if (c < 0) {
      return toHour > other.fromHour;
    } else if (c > 0) {
      return other.toHour > fromHour;
    } else {
      return true; // equal means overlapping
    }
  }

  static TimeRange of(int from, int to) {
    if (from >= to || (from | to) < 0 || from > 24 || to > 24) {
      throw new IllegalArgumentException("illegal time range: [" + from + ", " + to + "]");
    }
    return new TimeRange(from, to);
  }

  static TimeRange ofDuration(int fromHour, int duration) {
    return of(fromHour, fromHour + duration);
  }

  TimeRange following(int duration) {
    return of(toHour, toHour + duration);
  }

  TimeRange following(int delay, int duration) {
    return of(toHour + delay, toHour + delay + duration);
  }
}
