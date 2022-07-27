package com.github.mizosoft.timetablegenerator.old;

import com.github.mizosoft.timetablegenerator.old.model.TimeRange;

public class OverlappingRangeException extends RuntimeException {

  public OverlappingRangeException(String message) {
    super(message);
  }

  public OverlappingRangeException(TimeRange range) {
    // TODO implement
  }
}
