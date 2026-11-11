package com.agnitio.domain.photographer;

public class PhotographerNotFoundException extends RuntimeException {
    public PhotographerNotFoundException(PhotographerId id) {
        super("Photographer not found with id: " + id);
    }

    public PhotographerNotFoundException(String email) {
        super("Photographer not found with email: " + email);
    }
}
