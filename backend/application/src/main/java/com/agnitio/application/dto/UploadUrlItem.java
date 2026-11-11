package com.agnitio.application.dto;

import com.agnitio.domain.photo.PhotoId;

import java.util.Objects;

public record UploadUrlItem(
        PhotoId photoId,
        String fileName,
        String uploadUrl,
        String storageKey
) {
    public UploadUrlItem {
        Objects.requireNonNull(photoId, "photoId must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(uploadUrl, "uploadUrl must not be null");
        Objects.requireNonNull(storageKey, "storageKey must not be null");
    }
}
