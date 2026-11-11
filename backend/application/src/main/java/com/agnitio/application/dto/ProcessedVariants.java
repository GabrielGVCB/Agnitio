package com.agnitio.application.dto;

import java.util.Objects;

public record ProcessedVariants(byte[] thumbnailWebp, byte[] previewWebp, int width, int height) {
    public ProcessedVariants {
        Objects.requireNonNull(thumbnailWebp, "thumbnailWebp must not be null");
        Objects.requireNonNull(previewWebp, "previewWebp must not be null");
    }
}
