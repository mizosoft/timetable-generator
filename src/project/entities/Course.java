package project.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import project.entities.CourseAttendable.Type;

public final class Course {

  private final int id;
  private final String name;
  private final List<CourseAttendable> attendables;

  private Course(int id, String name, List<CourseAttendable> attendables) {
    this.id = id;
    this.name = name;
    this.attendables = List.copyOf(attendables);
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<CourseAttendable> getAttendables() {
    return attendables;
  }

  public List<Lecture> getLectures() {
    return attendables.stream()
        .filter(a -> a.getType() == Type.LECTURE)
        .map(Lecture.class::cast)
        .collect(Collectors.toUnmodifiableList());
  }

  public List<Lab> getLabs() {
    return attendables.stream()
        .filter(a -> a.getType() == Type.LAB)
        .map(Lab.class::cast)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public String toString() {
    return "Course{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", labs=" + getLabs() +
        ", lectures=" + getLectures() +
        '}';
  }

  public static final class Builder {

    private int id;
    private String name;
    private final List<CourseAttendable> attendables = new ArrayList<>();

    public Builder setId(int id) {
      this.id = id;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }
    
    public Builder addAttendables(CourseAttendable... attendables) {
        this.attendables.addAll(List.of(attendables));
        return this;
    }

    public Builder addAttendable(CourseAttendable attendable) {
      attendables.add(attendable);
      return this;
    }

//    public List<CourseAttendable> getAddedAttendables() {
//      return List.copyOf(attendables);
//    }

    public Course build() {
      return new Course(id, name, attendables);
    }
  }
}
