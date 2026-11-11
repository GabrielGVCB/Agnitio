package com.agnitio.application.usecase.auth;

import com.agnitio.application.command.AuthenticatePhotographerCommand;
import com.agnitio.application.dto.AuthTokens;
import com.agnitio.application.ports.output.PasswordEncoderPort;
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
class AuthenticatePhotographerUseCaseTest {

    @Mock
    private PhotographerRepository photographerRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private RefreshTokenStorage refreshTokenStorage;

    private AuthenticatePhotographerUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticatePhotographerUseCase(
                photographerRepository,
                passwordEncoder,
                tokenService,
                refreshTokenStorage
        );
    }

    @Test
    void shouldAuthenticateSuccessfully() {
        Photographer photographer = Photographer.create(
                PhotographerId.generate(),
                "gabriel@example.com",
                "hashed-pw",
                "Gabriel",
                PhotographerRole.PHOTOGRAPHER
        );

        when(photographerRepository.findByEmail("gabriel@example.com")).thenReturn(Optional.of(photographer));
        when(passwordEncoder.matches("raw-pw", "hashed-pw")).thenReturn(true);
        when(tokenService.generateAccessToken(photographer)).thenReturn("access-token-xyz");
        when(tokenService.generateRefreshToken()).thenReturn("refresh-token-123");

        AuthTokens tokens = useCase.execute(new AuthenticatePhotographerCommand("gabriel@example.com", "raw-pw"));

        assertThat(tokens.accessToken()).isEqualTo("access-token-xyz");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-token-123");
        verify(refreshTokenStorage).store(eq("refresh-token-123"), eq(photographer.getId()), any());
    }

    @Test
    void shouldFailWhenPasswordDoesNotMatch() {
        Photographer photographer = Photographer.create(
                PhotographerId.generate(),
                "gabriel@example.com",
                "hashed-pw",
                "Gabriel",
                PhotographerRole.PHOTOGRAPHER
        );

        when(photographerRepository.findByEmail("gabriel@example.com")).thenReturn(Optional.of(photographer));
        when(passwordEncoder.matches("wrong-pw", "hashed-pw")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new AuthenticatePhotographerCommand("gabriel@example.com", "wrong-pw")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
