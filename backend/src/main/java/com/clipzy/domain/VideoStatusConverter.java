package com.clipzy.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class VideoStatusConverter implements AttributeConverter<VideoStatus, String> {

  @Override
  public String convertToDatabaseColumn(VideoStatus attribute) {
    return attribute == null ? null : attribute.toDb();
  }

  @Override
  public VideoStatus convertToEntityAttribute(String dbData) {
    return dbData == null ? null : VideoStatus.fromDb(dbData);
  }
}
