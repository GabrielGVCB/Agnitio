package com.agnitio.domain.event;

public class UnauthorizedEventAccessException extends RuntimeException {
    public UnauthorizedEventAccessException(String message) {
        super(message);
    }
}
