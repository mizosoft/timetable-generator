package project;

import project.entities.TimeRange;

public class OverlappingRangeException extends RuntimeException {

  public OverlappingRangeException(String message) {
    super(message);
  }

  public OverlappingRangeException(TimeRange range) {
    // TODO implement
  }
}
