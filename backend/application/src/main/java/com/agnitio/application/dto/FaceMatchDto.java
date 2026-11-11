package com.agnitio.application.dto;

import java.util.Objects;
import java.util.UUID;

public record FaceMatchDto(
        UUID photoId,
        double similarity
) {
    public FaceMatchDto {
        Objects.requireNonNull(photoId, "photoId must not be null");
    }
}
