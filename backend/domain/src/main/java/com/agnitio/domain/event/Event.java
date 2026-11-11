package com.agnitio.domain.event;

import com.agnitio.domain.photographer.PhotographerId;

import java.time.Instant;
import java.util.Objects;

public class Event {
    private final EventId id;
    private final PhotographerId photographerId;
    private String name;
    private EventType type;
    private EventVisibility visibility;
    private String accessCode;
    private int biometricRetentionDays;
    private EventStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Event(
            EventId id,
            PhotographerId photographerId,
            String name,
            EventType type,
            EventVisibility visibility,
            String accessCode,
            int biometricRetentionDays,
            EventStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.photographerId = Objects.requireNonNull(photographerId, "photographerId must not be null");
        this.name = validateName(name);
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.visibility = Objects.requireNonNull(visibility, "visibility must not be null");
        this.accessCode = validateAccessCode(visibility, accessCode);
        this.biometricRetentionDays = validateRetentionDays(biometricRetentionDays);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Event create(
            EventId id,
            PhotographerId photographerId,
            String name,
            EventType type,
            EventVisibility visibility,
            String accessCode,
            int biometricRetentionDays
    ) {
        Instant now = Instant.now();
        return new Event(
                id,
                photographerId,
                name,
                type,
                visibility,
                accessCode,
                biometricRetentionDays,
                EventStatus.ACTIVE,
                now,
                now
        );
    }

    public void update(
            String name,
            EventType type,
            EventVisibility visibility,
            String accessCode,
            int biometricRetentionDays
    ) {
        this.name = validateName(name);
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.visibility = Objects.requireNonNull(visibility, "visibility must not be null");
        this.accessCode = validateAccessCode(visibility, accessCode);
        this.biometricRetentionDays = validateRetentionDays(biometricRetentionDays);
        this.updatedAt = Instant.now();
    }

    public void archive() {
        if (this.status == EventStatus.ARCHIVED) {
            throw new InvalidEventException("Event is already archived");
        }
        this.status = EventStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }

    public boolean isOwnedBy(PhotographerId photographerId) {
        return this.photographerId.equals(photographerId);
    }

    public boolean verifyAccess(String providedCode) {
        if (this.visibility == EventVisibility.PUBLIC) {
            return true;
        }
        return this.accessCode != null && this.accessCode.equals(providedCode);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidEventException("Event name must not be blank");
        }
        return name.trim();
    }

    private static String validateAccessCode(EventVisibility visibility, String accessCode) {
        if (visibility == EventVisibility.PRIVATE) {
            if (accessCode == null || accessCode.isBlank()) {
                throw new InvalidEventException("Access code is required for private events");
            }
            return accessCode.trim();
        }
        return null;
    }

    private static int validateRetentionDays(int days) {
        if (days < 1) {
            throw new InvalidEventException("Biometric retention days must be at least 1");
        }
        return days;
    }

    public EventId getId() {
        return id;
    }

    public PhotographerId getPhotographerId() {
        return photographerId;
    }

    public String getName() {
        return name;
    }

    public EventType getType() {
        return type;
    }

    public EventVisibility getVisibility() {
        return visibility;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public int getBiometricRetentionDays() {
        return biometricRetentionDays;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
