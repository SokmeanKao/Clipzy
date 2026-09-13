package com.clipzy.dto;

import com.clipzy.domain.VideoStatus;
import com.clipzy.domain.VideoVisibility;
import java.time.Instant;
import java.util.UUID;

public record VideoResponse(
    UUID id,
    UUID ownerId,
    String ownerDisplayName,
    String title,
    String description,
    VideoStatus status,
    VideoVisibility visibility,
    String manifestPath,
    String manifestUrl,
    String thumbnailPath,
    long viewCount,
    Instant publishedAt,
    Instant createdAt
) {
}
