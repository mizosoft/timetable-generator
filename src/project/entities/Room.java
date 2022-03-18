package project.entities;

public abstract class Room {

  private final int id;
  private final String name;
  private final int numberOfChairs;

  Room(int id, String name, int numberOfChairs) {
    this.id = id;
    this.name = name;
    this.numberOfChairs = numberOfChairs;
  }

  public abstract Type getType();

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getNumberOfChairs() {
    return numberOfChairs;
  }

  @Override
  public String toString() {
    return "Room{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", numberOfChairs=" + numberOfChairs +
        ", type=" + getType() +
        '}';
  }

  public enum Type {
    CLASSROOM,
    LABORATORY
  }

  public abstract static class Builder<R extends Room> {

    int id;
    String name;
    int numberOfChairs;

    public Builder<R> setId(int id) {
      this.id = id;
      return this;
    }

    public Builder<R> setName(String name) {
      this.name = name;
      return this;
    }

    public Builder<R> setNumberOfChairs(int numberOfChairs) {
      this.numberOfChairs = numberOfChairs;
      return this;
    }

    public abstract R build();
  }
}
