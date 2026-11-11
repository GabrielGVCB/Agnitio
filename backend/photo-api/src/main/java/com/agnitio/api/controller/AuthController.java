package com.agnitio.api.controller;

import com.agnitio.api.controller.dto.AuthResponseDto;
import com.agnitio.api.controller.dto.LoginRequestDto;
import com.agnitio.api.controller.dto.PhotographerResponseDto;
import com.agnitio.api.controller.dto.RegisterRequestDto;
import com.agnitio.application.command.AuthenticatePhotographerCommand;
import com.agnitio.application.command.RegisterPhotographerCommand;
import com.agnitio.application.dto.AuthTokens;
import com.agnitio.application.ports.output.PhotographerRepository;
import com.agnitio.application.usecase.auth.AuthenticatePhotographerUseCase;
import com.agnitio.application.usecase.auth.LogoutUseCase;
import com.agnitio.application.usecase.auth.RefreshTokenUseCase;
import com.agnitio.application.usecase.auth.RegisterPhotographerUseCase;
import com.agnitio.domain.photographer.InvalidCredentialsException;
import com.agnitio.domain.photographer.Photographer;
import com.agnitio.domain.photographer.PhotographerId;
import com.agnitio.domain.photographer.PhotographerNotFoundException;
import com.agnitio.infrastructure.redis.RedisRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final Duration REFRESH_COOKIE_MAX_AGE = Duration.ofDays(7);

    private final RegisterPhotographerUseCase registerUseCase;
    private final AuthenticatePhotographerUseCase authenticateUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final PhotographerRepository photographerRepository;
    private final RedisRateLimiter rateLimiter;

    public AuthController(
            RegisterPhotographerUseCase registerUseCase,
            AuthenticatePhotographerUseCase authenticateUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase,
            PhotographerRepository photographerRepository,
            RedisRateLimiter rateLimiter
    ) {
        this.registerUseCase = Objects.requireNonNull(registerUseCase, "registerUseCase must not be null");
        this.authenticateUseCase = Objects.requireNonNull(authenticateUseCase, "authenticateUseCase must not be null");
        this.refreshTokenUseCase = Objects.requireNonNull(refreshTokenUseCase, "refreshTokenUseCase must not be null");
        this.logoutUseCase = Objects.requireNonNull(logoutUseCase, "logoutUseCase must not be null");
        this.photographerRepository = Objects.requireNonNull(photographerRepository, "photographerRepository must not be null");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter must not be null");
    }

    @PostMapping("/register")
    public ResponseEntity<PhotographerResponseDto> register(@RequestBody @Valid RegisterRequestDto request) {
        RegisterPhotographerCommand command = new RegisterPhotographerCommand(
                request.email(),
                request.password(),
                request.name()
        );
        Photographer photographer = registerUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(PhotographerResponseDto.fromDomain(photographer));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @RequestBody @Valid LoginRequestDto request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = extractClientIp(servletRequest);
        if (!rateLimiter.tryAcquire("login:" + clientIp, 10, Duration.ofMinutes(1))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        AuthenticatePhotographerCommand command = new AuthenticatePhotographerCommand(
                request.email(),
                request.password()
        );

        AuthTokens tokens = authenticateUseCase.execute(command);

        Photographer photographer = photographerRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(InvalidCredentialsException::new);

        ResponseCookie cookie = createRefreshTokenCookie(tokens.refreshToken(), REFRESH_COOKIE_MAX_AGE);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponseDto(
                        tokens.accessToken(),
                        tokens.expiresInSeconds(),
                        PhotographerResponseDto.fromDomain(photographer)
                ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidCredentialsException();
        }

        AuthTokens tokens = refreshTokenUseCase.execute(refreshToken);

        ResponseCookie cookie = createRefreshTokenCookie(tokens.refreshToken(), REFRESH_COOKIE_MAX_AGE);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponseDto(
                        tokens.accessToken(),
                        tokens.expiresInSeconds(),
                        null
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) {
        if (refreshToken != null) {
            logoutUseCase.execute(refreshToken);
        }

        ResponseCookie clearCookie = createRefreshTokenCookie("", Duration.ZERO);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<PhotographerResponseDto> getCurrentPhotographer(
            @AuthenticationPrincipal String photographerId
    ) {
        if (photographerId == null) {
            throw new InvalidCredentialsException();
        }

        Photographer photographer = photographerRepository.findById(PhotographerId.of(photographerId))
                .orElseThrow(() -> new PhotographerNotFoundException(PhotographerId.of(photographerId)));

        return ResponseEntity.ok(PhotographerResponseDto.fromDomain(photographer));
    }

    private ResponseCookie createRefreshTokenCookie(String token, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(false) // Can be made dynamic for prod SSL
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
