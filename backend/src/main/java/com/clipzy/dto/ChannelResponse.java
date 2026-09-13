package com.clipzy.dto;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;

public record ChannelResponse(
    UUID id,
    String displayName,
    String avatarUrl,
    Instant createdAt,
    long subscriberCount,
    Page<VideoResponse> videos
) {
}
