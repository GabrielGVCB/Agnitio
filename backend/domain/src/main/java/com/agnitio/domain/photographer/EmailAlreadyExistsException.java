package com.agnitio.domain.photographer;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("A photographer with email '" + email + "' already exists");
    }
}
