package com.agnitio.infrastructure.persistence.event;

import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.event.EventStatus;
import com.agnitio.domain.event.EventType;
import com.agnitio.domain.event.EventVisibility;
import com.agnitio.domain.photographer.PhotographerId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class EventJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "photographer_id", nullable = false)
    private UUID photographerId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 50)
    private EventVisibility visibility;

    @Column(name = "access_code", length = 100)
    private String accessCode;

    @Column(name = "biometric_retention_days", nullable = false)
    private int biometricRetentionDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private EventStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public EventJpaEntity() {
    }

    public EventJpaEntity(
            UUID id,
            UUID photographerId,
            String name,
            EventType type,
            EventVisibility visibility,
            String accessCode,
            int biometricRetentionDays,
            EventStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.photographerId = photographerId;
        this.name = name;
        this.type = type;
        this.visibility = visibility;
        this.accessCode = accessCode;
        this.biometricRetentionDays = biometricRetentionDays;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EventJpaEntity fromDomain(Event event) {
        return new EventJpaEntity(
                event.getId().value(),
                event.getPhotographerId().value(),
                event.getName(),
                event.getType(),
                event.getVisibility(),
                event.getAccessCode(),
                event.getBiometricRetentionDays(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    public Event toDomain() {
        return new Event(
                new EventId(this.id),
                new PhotographerId(this.photographerId),
                this.name,
                this.type,
                this.visibility,
                this.accessCode,
                this.biometricRetentionDays,
                this.status,
                this.createdAt,
                this.updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPhotographerId() {
        return photographerId;
    }

    public void setPhotographerId(UUID photographerId) {
        this.photographerId = photographerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public EventVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(EventVisibility visibility) {
        this.visibility = visibility;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public int getBiometricRetentionDays() {
        return biometricRetentionDays;
    }

    public void setBiometricRetentionDays(int biometricRetentionDays) {
        this.biometricRetentionDays = biometricRetentionDays;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
