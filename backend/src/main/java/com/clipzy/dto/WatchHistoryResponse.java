package com.clipzy.dto;

import java.time.Instant;
import java.util.UUID;

public record WatchHistoryResponse(
    UUID id,
    UUID videoId,
    String title,
    int progressSeconds,
    Instant lastWatchedAt
) {
}
