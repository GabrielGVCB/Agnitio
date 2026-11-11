package com.agnitio.domain.photo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PhotoUploadedEvent(
        UUID messageId,
        UUID correlationId,
        Instant occurredAt,
        int schemaVersion,
        UUID photoId,
        UUID eventId,
        String storageKey,
        String originalFileName
) {
    public PhotoUploadedEvent {
        Objects.requireNonNull(messageId, "messageId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(photoId, "photoId must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(storageKey, "storageKey must not be null");
        Objects.requireNonNull(originalFileName, "originalFileName must not be null");
    }

    public static PhotoUploadedEvent of(Photo photo, UUID correlationId) {
        return new PhotoUploadedEvent(
                UUID.randomUUID(),
                correlationId != null ? correlationId : UUID.randomUUID(),
                Instant.now(),
                1,
                photo.getId().value(),
                photo.getEventId().value(),
                photo.getOriginalStorageKey(),
                photo.getOriginalFileName()
        );
    }
}
