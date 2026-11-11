package com.agnitio.api.controller.dto;

import java.util.List;
import java.util.UUID;

public record SearchSelfieResponseDto(
        UUID eventId,
        int totalMatches,
        List<SearchSelfieMatchItemDto> matches
) {
    public record SearchSelfieMatchItemDto(
            UUID photoId,
            String previewUrl,
            String thumbnailUrl,
            double similarity
    ) {}
}
