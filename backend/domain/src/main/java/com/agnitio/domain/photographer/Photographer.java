package com.agnitio.domain.photographer;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

public class Photographer {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private final PhotographerId id;
    private String email;
    private String passwordHash;
    private String name;
    private final PhotographerRole role;
    private PhotographerStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Photographer(
            PhotographerId id,
            String email,
            String passwordHash,
            String name,
            PhotographerRole role,
            PhotographerStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.email = validateEmail(email);
        this.passwordHash = validatePasswordHash(passwordHash);
        this.name = validateName(name);
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Photographer create(
            PhotographerId id,
            String email,
            String passwordHash,
            String name,
            PhotographerRole role
    ) {
        Instant now = Instant.now();
        return new Photographer(
                id,
                email,
                passwordHash,
                name,
                role != null ? role : PhotographerRole.PHOTOGRAPHER,
                PhotographerStatus.ACTIVE,
                now,
                now
        );
    }

    public void updateProfile(String name, String email) {
        this.name = validateName(name);
        this.email = validateEmail(email);
        this.updatedAt = Instant.now();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = validatePasswordHash(newPasswordHash);
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        if (this.status == PhotographerStatus.INACTIVE) {
            throw new InvalidPhotographerStateException("Photographer is already inactive");
        }
        this.status = PhotographerStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (this.status == PhotographerStatus.ACTIVE) {
            throw new InvalidPhotographerStateException("Photographer is already active");
        }
        this.status = PhotographerStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == PhotographerStatus.ACTIVE;
    }

    private static String validateEmail(String email) {
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        return email.trim().toLowerCase();
    }

    private static String validatePasswordHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("Password hash must not be blank");
        }
        return hash;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        return name.trim();
    }

    public PhotographerId getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public PhotographerRole getRole() {
        return role;
    }

    public PhotographerStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
