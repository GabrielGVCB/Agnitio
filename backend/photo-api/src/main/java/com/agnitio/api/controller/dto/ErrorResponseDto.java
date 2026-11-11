package com.agnitio.api.controller.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponseDto(
        int status,
        String error,
        String message,
        List<String> details,
        Instant timestamp
) {
    public static ErrorResponseDto of(int status, String error, String message) {
        return new ErrorResponseDto(status, error, message, List.of(), Instant.now());
    }

    public static ErrorResponseDto of(int status, String error, String message, List<String> details) {
        return new ErrorResponseDto(status, error, message, details, Instant.now());
    }
}
