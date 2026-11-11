package com.agnitio.application.command;

import java.util.Objects;
import java.util.UUID;

public record ProcessPhotoCommand(
        UUID photoId,
        UUID correlationId
) {
    public ProcessPhotoCommand {
        Objects.requireNonNull(photoId, "photoId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
    }
}
