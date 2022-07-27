package com.github.mizosoft.timetablegenerator.old.model;

public final class Classroom extends Room {

  private Classroom(int id, String name, int numberOfChairs) {
    super(id, name, numberOfChairs);
  }

  @Override
  public Type getType() {
    return Type.CLASSROOM;
  }

  public static final class Builder extends Room.Builder<Classroom> {

    @Override
    public Classroom build() {
      return new Classroom(id, name, numberOfChairs);
    }
  }
}
