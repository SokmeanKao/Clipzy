package com.clipzy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProgressRequest(
    @NotNull @Min(0) Integer progressSeconds
) {
}
