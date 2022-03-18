package project;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {

  public static int modInc(int x, int len) {
    return (x + 1) % len;
  }

  public static <U, V> Pair<U, V> pair(U first, V second) {
    return new Pair<>(first, second);
  }

  public static String capitalize(String value) {
    return Stream.of(value.split("_"))
        .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase())
        .collect(Collectors.joining(" "));
  }

  public static String capitalizeEnum(Enum<?> e) {
    return capitalize(e.name());
  }

  @SuppressWarnings("unchecked")
  public static <T> T castUnchecked(Object v) {
    return (T) v;
  }

  public static final class Pair<U, V> {
    private final U first;
    private final V second;

    private Pair(U first, V second) {
      this.first = first;
      this.second = second;
    }

    public U getFirst() {
      return first;
    }

    public V getSecond() {
      return second;
    }

    @Override
    public String toString() {
      return "[" + first + ", " + second + "]";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Pair)) {
        return false;
      }
      Pair<?, ?> pair = (Pair<?, ?>) o;
      return Objects.equals(first, pair.first) &&
          Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(first, second);
    }
  }
}
