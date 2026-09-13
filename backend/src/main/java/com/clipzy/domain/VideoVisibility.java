package com.clipzy.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VideoVisibility {
  PUBLIC,
  UNLISTED,
  PRIVATE;

  @JsonValue
  public String toDb() {
    return name().toLowerCase();
  }

  @JsonCreator
  public static VideoVisibility fromDb(String value) {
    return VideoVisibility.valueOf(value.toUpperCase());
  }
}
