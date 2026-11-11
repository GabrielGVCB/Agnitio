package com.agnitio.domain.photographer;

public class InvalidPhotographerStateException extends RuntimeException {
    public InvalidPhotographerStateException(String message) {
        super(message);
    }
}
