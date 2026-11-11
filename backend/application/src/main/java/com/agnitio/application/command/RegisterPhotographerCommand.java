package com.agnitio.application.command;

import java.util.Objects;

public record RegisterPhotographerCommand(String email, String password, String name) {
    public RegisterPhotographerCommand {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(password, "password must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }
}
