package com.clipzy.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class JobStatusConverter implements AttributeConverter<JobStatus, String> {

  @Override
  public String convertToDatabaseColumn(JobStatus attribute) {
    return attribute == null ? null : attribute.toDb();
  }

  @Override
  public JobStatus convertToEntityAttribute(String dbData) {
    return dbData == null ? null : JobStatus.fromDb(dbData);
  }
}
