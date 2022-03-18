package project.entities;

import java.time.Duration;

public final class Lecture extends CourseAttendable {

  Lecture(Teacher teacher, Room room, Duration duration) {
    super(teacher, room, duration);
  }

  @Override
  public Type getType() {
    return Type.LECTURE;
  }

  public static final class Builder extends CourseAttendable.Builder<Lecture> {

    @Override
    public Lecture build() {
      return new Lecture(teacher, room, duration);
    }
  }
}
