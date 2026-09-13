package com.clipzy.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VideoStatus {
  UPLOADING,
  PROCESSING,
  READY,
  FAILED;

  @JsonValue
  public String toDb() {
    return name().toLowerCase();
  }

  @JsonCreator
  public static VideoStatus fromDb(String value) {
    return VideoStatus.valueOf(value.toUpperCase());
  }
}
