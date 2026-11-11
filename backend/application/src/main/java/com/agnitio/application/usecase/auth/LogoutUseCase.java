package com.agnitio.application.usecase.auth;

import com.agnitio.application.ports.output.RefreshTokenStorage;

import java.util.Objects;

public class LogoutUseCase {
    private final RefreshTokenStorage refreshTokenStorage;

    public LogoutUseCase(RefreshTokenStorage refreshTokenStorage) {
        this.refreshTokenStorage = Objects.requireNonNull(refreshTokenStorage, "refreshTokenStorage must not be null");
    }

    public void execute(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenStorage.revoke(refreshToken.trim());
        }
    }
}
