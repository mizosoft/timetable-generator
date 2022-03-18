package project.entities;

public final class Laboratory extends Room {

  private Laboratory(int id, String name, int numberOfChairs) {
    super(id, name, numberOfChairs);
  }

  @Override
  public Type getType() {
    return Type.LABORATORY;
  }

  public static final class Builder extends Room.Builder<Laboratory> {

    @Override
    public Laboratory build() {
      return new Laboratory(id, name, numberOfChairs);
    }
  }
}
