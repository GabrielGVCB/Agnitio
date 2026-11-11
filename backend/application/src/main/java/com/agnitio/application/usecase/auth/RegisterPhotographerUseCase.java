package com.agnitio.application.usecase.auth;

import com.agnitio.application.command.RegisterPhotographerCommand;
import com.agnitio.application.ports.output.PasswordEncoderPort;
import com.agnitio.application.ports.output.PhotographerRepository;
import com.agnitio.domain.photographer.EmailAlreadyExistsException;
import com.agnitio.domain.photographer.Photographer;
import com.agnitio.domain.photographer.PhotographerId;
import com.agnitio.domain.photographer.PhotographerRole;

import java.util.Objects;

public class RegisterPhotographerUseCase {
    private final PhotographerRepository photographerRepository;
    private final PasswordEncoderPort passwordEncoder;

    public RegisterPhotographerUseCase(
            PhotographerRepository photographerRepository,
            PasswordEncoderPort passwordEncoder
    ) {
        this.photographerRepository = Objects.requireNonNull(photographerRepository, "photographerRepository must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
    }

    public Photographer execute(RegisterPhotographerCommand command) {
        if (photographerRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException(command.email());
        }

        String passwordHash = passwordEncoder.encode(command.password());
        Photographer photographer = Photographer.create(
                PhotographerId.generate(),
                command.email(),
                passwordHash,
                command.name(),
                PhotographerRole.PHOTOGRAPHER
        );

        return photographerRepository.save(photographer);
    }
}
