package com.agnitio.application.usecase.auth;

import com.agnitio.application.command.RegisterPhotographerCommand;
import com.agnitio.application.ports.output.PasswordEncoderPort;
import com.agnitio.application.ports.output.PhotographerRepository;
import com.agnitio.domain.photographer.EmailAlreadyExistsException;
import com.agnitio.domain.photographer.Photographer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterPhotographerUseCaseTest {

    @Mock
    private PhotographerRepository photographerRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    private RegisterPhotographerUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterPhotographerUseCase(photographerRepository, passwordEncoder);
    }

    @Test
    void shouldRegisterPhotographerSuccessfully() {
        RegisterPhotographerCommand command = new RegisterPhotographerCommand(
                "gabriel@example.com",
                "Secret123!",
                "Gabriel"
        );

        when(photographerRepository.existsByEmail("gabriel@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed-secret");
        when(photographerRepository.save(any(Photographer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Photographer result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("gabriel@example.com");
        assertThat(result.getName()).isEqualTo("Gabriel");
        assertThat(result.getPasswordHash()).isEqualTo("hashed-secret");
        verify(photographerRepository).save(any(Photographer.class));
    }

    @Test
    void shouldFailWhenEmailAlreadyExists() {
        RegisterPhotographerCommand command = new RegisterPhotographerCommand(
                "gabriel@example.com",
                "Secret123!",
                "Gabriel"
        );

        when(photographerRepository.existsByEmail("gabriel@example.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }
}
