package com.agnitio.application.dto;

import com.agnitio.domain.photo.PhotoId;
import com.agnitio.domain.photo.PhotoStatus;

import java.time.Instant;
import java.util.Objects;

public record EventGalleryItem(
        PhotoId photoId,
        String originalFileName,
        PhotoStatus status,
        String originalUrl,
        String previewUrl,
        String thumbnailUrl,
        Instant createdAt
) {
    public EventGalleryItem {
        Objects.requireNonNull(photoId, "photoId must not be null");
        Objects.requireNonNull(originalFileName, "originalFileName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
