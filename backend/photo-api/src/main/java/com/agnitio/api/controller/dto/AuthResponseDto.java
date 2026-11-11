package com.agnitio.api.controller.dto;

public record AuthResponseDto(
        String accessToken,
        long expiresInSeconds,
        PhotographerResponseDto photographer
) {
}
