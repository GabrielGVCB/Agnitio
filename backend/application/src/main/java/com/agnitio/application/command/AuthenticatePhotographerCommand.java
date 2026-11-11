package com.agnitio.application.command;

import java.util.Objects;

public record AuthenticatePhotographerCommand(String email, String password) {
    public AuthenticatePhotographerCommand {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(password, "password must not be null");
    }
}
