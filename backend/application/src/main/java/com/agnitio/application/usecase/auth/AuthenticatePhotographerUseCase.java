package com.agnitio.application.usecase.auth;

import com.agnitio.application.command.AuthenticatePhotographerCommand;
import com.agnitio.application.dto.AuthTokens;
import com.agnitio.application.ports.output.PasswordEncoderPort;
import com.agnitio.application.ports.output.PhotographerRepository;
import com.agnitio.application.ports.output.RefreshTokenStorage;
import com.agnitio.application.ports.output.TokenService;
import com.agnitio.domain.photographer.InvalidCredentialsException;
import com.agnitio.domain.photographer.InvalidPhotographerStateException;
import com.agnitio.domain.photographer.Photographer;

import java.time.Duration;
import java.util.Objects;

public class AuthenticatePhotographerUseCase {
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final long ACCESS_TOKEN_EXPIRES_IN_SECONDS = 900L; // 15 minutes

    private final PhotographerRepository photographerRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenStorage refreshTokenStorage;

    public AuthenticatePhotographerUseCase(
            PhotographerRepository photographerRepository,
            PasswordEncoderPort passwordEncoder,
            TokenService tokenService,
            RefreshTokenStorage refreshTokenStorage
    ) {
        this.photographerRepository = Objects.requireNonNull(photographerRepository, "photographerRepository must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
        this.tokenService = Objects.requireNonNull(tokenService, "tokenService must not be null");
        this.refreshTokenStorage = Objects.requireNonNull(refreshTokenStorage, "refreshTokenStorage must not be null");
    }

    public AuthTokens execute(AuthenticatePhotographerCommand command) {
        Photographer photographer = photographerRepository.findByEmail(command.email().trim().toLowerCase())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.password(), photographer.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!photographer.isActive()) {
            throw new InvalidPhotographerStateException("Photographer account is inactive");
        }

        String accessToken = tokenService.generateAccessToken(photographer);
        String refreshToken = tokenService.generateRefreshToken();

        refreshTokenStorage.store(refreshToken, photographer.getId(), REFRESH_TOKEN_TTL);

        return new AuthTokens(accessToken, refreshToken, ACCESS_TOKEN_EXPIRES_IN_SECONDS);
    }
}
