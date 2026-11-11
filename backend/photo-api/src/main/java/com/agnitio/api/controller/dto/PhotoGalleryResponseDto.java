package com.agnitio.api.controller.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PhotoGalleryResponseDto(
        UUID eventId,
        int totalPhotos,
        List<PhotoGalleryItemDto> photos
) {
    public record PhotoGalleryItemDto(
            UUID photoId,
            String originalFileName,
            String status,
            String originalUrl,
            String previewUrl,
            String thumbnailUrl,
            Instant createdAt
    ) {}
}
