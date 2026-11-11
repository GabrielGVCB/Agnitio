package com.agnitio.application.dto;

import java.util.Objects;
import java.util.UUID;

public record SelfieSearchResultDto(
        UUID photoId,
        String previewUrl,
        String thumbnailUrl,
        double similarity
) {
    public SelfieSearchResultDto {
        Objects.requireNonNull(photoId, "photoId must not be null");
        Objects.requireNonNull(previewUrl, "previewUrl must not be null");
        Objects.requireNonNull(thumbnailUrl, "thumbnailUrl must not be null");
    }
}
