package com.agnitio.api.controller.dto;

import com.agnitio.domain.photographer.Photographer;

import java.time.Instant;
import java.util.UUID;

public record PhotographerResponseDto(
        UUID id,
        String email,
        String name,
        String role,
        String status,
        Instant createdAt
) {
    public static PhotographerResponseDto fromDomain(Photographer photographer) {
        return new PhotographerResponseDto(
                photographer.getId().value(),
                photographer.getEmail(),
                photographer.getName(),
                photographer.getRole().name(),
                photographer.getStatus().name(),
                photographer.getCreatedAt()
        );
    }
}
