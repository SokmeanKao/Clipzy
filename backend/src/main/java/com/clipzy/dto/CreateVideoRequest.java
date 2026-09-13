package com.clipzy.dto;

import com.clipzy.domain.VideoVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVideoRequest(
    @NotBlank @Size(max = 300) String title,
    @Size(max = 10000) String description,
    VideoVisibility visibility
) {
}
