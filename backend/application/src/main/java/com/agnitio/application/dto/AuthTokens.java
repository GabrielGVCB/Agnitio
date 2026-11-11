package com.agnitio.application.dto;

import java.util.Objects;

public record AuthTokens(String accessToken, String refreshToken, long expiresInSeconds) {
    public AuthTokens {
        Objects.requireNonNull(accessToken, "accessToken must not be null");
        Objects.requireNonNull(refreshToken, "refreshToken must not be null");
    }
}
