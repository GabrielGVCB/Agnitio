package com.agnitio.api.controller;

import java.time.Instant;
import java.util.UUID;

public record PhotoResponseDto(
        UUID id,
        UUID eventId,
        String originalFileName,
        String status,
        Instant createdAt
) {
}
