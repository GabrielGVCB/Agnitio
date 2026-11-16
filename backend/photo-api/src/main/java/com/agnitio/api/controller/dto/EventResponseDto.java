package com.agnitio.api.controller.dto;

import com.agnitio.domain.event.Event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EventResponseDto(
        UUID id,
        UUID photographerId,
        String name,
        String type,
        String visibility,
        boolean hasAccessCode,
        int biometricRetentionDays,
        String status,
        LocalDate eventDate,
        String city,
        Instant createdAt,
        Instant updatedAt
) {
    public static EventResponseDto fromDomain(Event event) {
        return new EventResponseDto(
                event.getId().value(),
                event.getPhotographerId().value(),
                event.getName(),
                event.getType().name(),
                event.getVisibility().name(),
                event.getAccessCode() != null && !event.getAccessCode().isBlank(),
                event.getBiometricRetentionDays(),
                event.getStatus().name(),
                event.getEventDate(),
                event.getCity(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
