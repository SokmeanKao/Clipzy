package com.clipzy.dto;

import java.util.UUID;

public record CreateVideoResponse(
    UUID id,
    String uploadUrl,
    String objectKey,
    String status
) {
}
