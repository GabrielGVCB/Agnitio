package com.agnitio.application.usecase.auth;

import com.agnitio.application.dto.AuthTokens;
import com.agnitio.application.ports.output.PhotographerRepository;
import com.agnitio.application.ports.output.RefreshTokenStorage;
import com.agnitio.application.ports.output.TokenService;
import com.agnitio.domain.photographer.InvalidCredentialsException;
import com.agnitio.domain.photographer.Photographer;
import com.agnitio.domain.photographer.PhotographerId;
import com.agnitio.domain.photographer.PhotographerRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock
    private PhotographerRepository photographerRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private RefreshTokenStorage refreshTokenStorage;

    private RefreshTokenUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RefreshTokenUseCase(photographerRepository, tokenService, refreshTokenStorage);
    }

    @Test
    void shouldRotateRefreshTokenSuccessfully() {
        PhotographerId photographerId = PhotographerId.generate();
        Photographer photographer = Photographer.create(
                photographerId,
                "gabriel@example.com",
                "hashed-pw",
                "Gabriel",
                PhotographerRole.PHOTOGRAPHER
        );

        when(refreshTokenStorage.findPhotographerIdByToken("old-token")).thenReturn(Optional.of(photographerId));
        when(photographerRepository.findById(photographerId)).thenReturn(Optional.of(photographer));
        when(tokenService.generateAccessToken(photographer)).thenReturn("new-access-token");
        when(tokenService.generateRefreshToken()).thenReturn("new-refresh-token");

        AuthTokens tokens = useCase.execute("old-token");

        assertThat(tokens.accessToken()).isEqualTo("new-access-token");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenStorage).revoke("old-token");
        verify(refreshTokenStorage).store(eq("new-refresh-token"), eq(photographerId), any());
    }

    @Test
    void shouldFailWhenOldTokenNotFound() {
        when(refreshTokenStorage.findPhotographerIdByToken("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("non-existent"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
