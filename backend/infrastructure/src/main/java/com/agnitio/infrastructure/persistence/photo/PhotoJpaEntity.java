package com.agnitio.infrastructure.persistence.photo;

import com.agnitio.domain.photo.PhotoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "photos")
public class PhotoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PhotoStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PhotoJpaEntity() {
    }

    public PhotoJpaEntity(UUID id, UUID eventId, String originalFileName, PhotoStatus status, Instant createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.originalFileName = originalFileName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public PhotoStatus getStatus() {
        return status;
    }

    public void setStatus(PhotoStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
