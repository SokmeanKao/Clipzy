package com.clipzy.dto;

public record ApiError(
    String message,
    int status
) {
}
