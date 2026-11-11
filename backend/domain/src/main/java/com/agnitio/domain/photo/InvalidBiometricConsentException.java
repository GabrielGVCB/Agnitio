package com.agnitio.domain.photo;

public class InvalidBiometricConsentException extends RuntimeException {
    public InvalidBiometricConsentException(String message) {
        super(message);
    }
}
