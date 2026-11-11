package com.agnitio.api.controller.dto;

import java.util.List;
import java.util.UUID;

public record UploadUrlsResponseDto(
        List<UploadUrlItemResponseDto> items
) {
    public record UploadUrlItemResponseDto(
            UUID photoId,
            String fileName,
            String uploadUrl,
            String storageKey
    ) {}
}
