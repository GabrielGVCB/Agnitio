package com.agnitio.application.usecase.auth;

import com.agnitio.application.dto.AuthTokens;
import com.agnitio.application.ports.output.PhotographerRepository;
import com.agnitio.application.ports.output.RefreshTokenStorage;
import com.agnitio.application.ports.output.TokenService;
import com.agnitio.domain.photographer.InvalidCredentialsException;
import com.agnitio.domain.photographer.Photographer;
import com.agnitio.domain.photographer.PhotographerId;

import java.time.Duration;
import java.util.Objects;

public class RefreshTokenUseCase {
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final long ACCESS_TOKEN_EXPIRES_IN_SECONDS = 900L;

    private final PhotographerRepository photographerRepository;
    private final TokenService tokenService;
    private final RefreshTokenStorage refreshTokenStorage;

    public RefreshTokenUseCase(
            PhotographerRepository photographerRepository,
            TokenService tokenService,
            RefreshTokenStorage refreshTokenStorage
    ) {
        this.photographerRepository = Objects.requireNonNull(photographerRepository, "photographerRepository must not be null");
        this.tokenService = Objects.requireNonNull(tokenService, "tokenService must not be null");
        this.refreshTokenStorage = Objects.requireNonNull(refreshTokenStorage, "refreshTokenStorage must not be null");
    }

    public AuthTokens execute(String oldRefreshToken) {
        if (oldRefreshToken == null || oldRefreshToken.isBlank()) {
            throw new InvalidCredentialsException();
        }

        PhotographerId photographerId = refreshTokenStorage.findPhotographerIdByToken(oldRefreshToken)
                .orElseThrow(InvalidCredentialsException::new);

        // Immediate revocation for strict rotation
        refreshTokenStorage.revoke(oldRefreshToken);

        Photographer photographer = photographerRepository.findById(photographerId)
                .orElseThrow(InvalidCredentialsException::new);

        if (!photographer.isActive()) {
            throw new InvalidCredentialsException();
        }

        String newAccessToken = tokenService.generateAccessToken(photographer);
        String newRefreshToken = tokenService.generateRefreshToken();

        refreshTokenStorage.store(newRefreshToken, photographer.getId(), REFRESH_TOKEN_TTL);

        return new AuthTokens(newAccessToken, newRefreshToken, ACCESS_TOKEN_EXPIRES_IN_SECONDS);
    }
}
