package com.clipzy.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    UUID videoId,
    UUID userId,
    String displayName,
    UUID parentId,
    String body,
    Instant createdAt
) {
}
