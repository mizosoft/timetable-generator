package com.github.mizosoft.timetablegenerator.old.model;

import java.time.Duration;

public class Lab extends CourseAttendable {

  Lab(Teacher teacher, Room room, Duration duration) {
    super(teacher, room, duration);
  }

  @Override
  public Type getType() {
    return Type.LAB;
  }

  public static final class Builder extends CourseAttendable.Builder<Lab> {

    @Override
    public Lab build() {
      return new Lab(teacher, room, duration);
    }
  }
}
