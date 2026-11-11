package com.agnitio.application.command;

import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photo.PhotoId;
import com.agnitio.domain.photographer.PhotographerId;

import java.util.List;
import java.util.Objects;

public record ConfirmPhotoUploadCommand(
        EventId eventId,
        PhotographerId photographerId,
        List<PhotoId> photoIds
) {
    public ConfirmPhotoUploadCommand {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(photographerId, "photographerId must not be null");
        Objects.requireNonNull(photoIds, "photoIds must not be null");
        if (photoIds.isEmpty()) {
            throw new IllegalArgumentException("photoIds list cannot be empty");
        }
    }
}
