package project.entities;

import java.time.Duration;

public abstract class CourseAttendable {

  private final Teacher teacher;
  private final Room room;
  private final Duration duration;

  CourseAttendable(Teacher teacher, Room room, Duration duration) {
    this.teacher = teacher;
    this.room = room;
    this.duration = duration;
  }

  public abstract Type getType();

  public Teacher getTeacher() {
    return teacher;
  }

  public Room getRoom() {
    return room;
  }

  public Duration getDuration() {
    return duration;
  }

  @Override
  public String toString() {
    return "CourseAttendable{" +
        "teacher=" + teacher +
        ", room=" + room +
        ", duration=" + duration +
        ", type=" + getType() +
        '}';
  }

  public enum Type {
    LECTURE,
    LAB
  }

  public abstract static class Builder<A extends CourseAttendable> {

    Teacher teacher;
    Room room;
    Duration duration;

    public Builder<A> setTeacher(Teacher teacher) {
      this.teacher = teacher;
      return this;
    }

    public Builder<A> setRoom(Room room) {
      this.room = room;
      return this;
    }

    public Builder<A> setDuration(Duration duration) {
      this.duration = duration;
      return this;
    }

    public abstract A build();
  }
}
