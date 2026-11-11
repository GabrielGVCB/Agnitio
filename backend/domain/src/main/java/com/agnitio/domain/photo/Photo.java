package com.agnitio.domain.photo;

import com.agnitio.domain.event.EventId;

import java.time.Instant;
import java.util.Objects;

public class Photo {
    private final PhotoId id;
    private final EventId eventId;
    private final String originalFileName;
    private final Instant createdAt;
    private PhotoStatus status;

    public Photo(PhotoId id, EventId eventId, String originalFileName, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.originalFileName = Objects.requireNonNull(originalFileName, "originalFileName must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.status = PhotoStatus.PENDING_UPLOAD;
    }

    public Photo(PhotoId id, EventId eventId, String originalFileName, Instant createdAt, PhotoStatus status) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.originalFileName = Objects.requireNonNull(originalFileName, "originalFileName must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public void markAsUploaded() {
        if (this.status != PhotoStatus.PENDING_UPLOAD) {
            throw new InvalidPhotoStateTransitionException(this.status, PhotoStatus.UPLOADED);
        }
        this.status = PhotoStatus.UPLOADED;
    }

    public void startProcessing() {
        if (this.status != PhotoStatus.UPLOADED) {
            throw new InvalidPhotoStateTransitionException(this.status, PhotoStatus.PROCESSING);
        }
        this.status = PhotoStatus.PROCESSING;
    }

    public void completeProcessing() {
        if (this.status != PhotoStatus.PROCESSING) {
            throw new InvalidPhotoStateTransitionException(this.status, PhotoStatus.READY);
        }
        this.status = PhotoStatus.READY;
    }

    public void markAsFailed() {
        if (this.status == PhotoStatus.READY) {
            throw new InvalidPhotoStateTransitionException(this.status, PhotoStatus.FAILED);
        }
        this.status = PhotoStatus.FAILED;
    }

    public PhotoId getId() {
        return id;
    }

    public EventId getEventId() {
        return eventId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public PhotoStatus getStatus() {
        return status;
    }

    public String getOriginalStorageKey() {
        String ext = getFileExtension(originalFileName);
        return "events/" + eventId.value() + "/original/" + id.value() + (ext.isEmpty() ? "" : "." + ext);
    }

    public String getPreviewStorageKey() {
        return "events/" + eventId.value() + "/preview/" + id.value() + ".webp";
    }

    public String getThumbnailStorageKey() {
        return "events/" + eventId.value() + "/thumb/" + id.value() + ".webp";
    }

    private static String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        return (lastDot > 0 && lastDot < filename.length() - 1) ? filename.substring(lastDot + 1).toLowerCase() : "";
    }
}
