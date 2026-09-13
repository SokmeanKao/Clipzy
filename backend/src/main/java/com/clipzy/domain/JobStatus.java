package com.clipzy.domain;

public enum JobStatus {
  PENDING,
  RUNNING,
  SUCCEEDED,
  FAILED;

  public String toDb() {
    return name().toLowerCase();
  }

  public static JobStatus fromDb(String value) {
    return JobStatus.valueOf(value.toUpperCase());
  }
}
