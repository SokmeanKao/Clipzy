package com.clipzy.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class VideoVisibilityConverter implements AttributeConverter<VideoVisibility, String> {

  @Override
  public String convertToDatabaseColumn(VideoVisibility attribute) {
    return attribute == null ? null : attribute.toDb();
  }

  @Override
  public VideoVisibility convertToEntityAttribute(String dbData) {
    return dbData == null ? null : VideoVisibility.fromDb(dbData);
  }
}
