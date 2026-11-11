package com.agnitio.domain.photo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PhotoProcessedEvent(
        UUID messageId,
        UUID correlationId,
        Instant occurredAt,
        int schemaVersion,
        UUID photoId,
        UUID eventId,
        String originalStorageKey,
        String previewStorageKey,
        String thumbnailStorageKey,
        int width,
        int height
) {
    public PhotoProcessedEvent {
        Objects.requireNonNull(messageId, "messageId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(photoId, "photoId must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(originalStorageKey, "originalStorageKey must not be null");
        Objects.requireNonNull(previewStorageKey, "previewStorageKey must not be null");
        Objects.requireNonNull(thumbnailStorageKey, "thumbnailStorageKey must not be null");
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }
    }

    public static PhotoProcessedEvent of(Photo photo, UUID correlationId, int width, int height) {
        return new PhotoProcessedEvent(
                UUID.randomUUID(),
                correlationId != null ? correlationId : UUID.randomUUID(),
                Instant.now(),
                1,
                photo.getId().value(),
                photo.getEventId().value(),
                photo.getOriginalStorageKey(),
                photo.getPreviewStorageKey(),
                photo.getThumbnailStorageKey(),
                width,
                height
        );
    }
}
