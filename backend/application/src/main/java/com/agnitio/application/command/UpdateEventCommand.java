package com.agnitio.application.command;

import com.agnitio.domain.event.EventId;
import com.agnitio.domain.event.EventType;
import com.agnitio.domain.event.EventVisibility;
import com.agnitio.domain.photographer.PhotographerId;

import java.time.LocalDate;
import java.util.Objects;

public record UpdateEventCommand(
        EventId eventId,
        PhotographerId photographerId,
        String name,
        EventType type,
        EventVisibility visibility,
        String accessCode,
        int biometricRetentionDays,
        LocalDate eventDate,
        String city
) {
    public UpdateEventCommand {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(photographerId, "photographerId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(visibility, "visibility must not be null");
    }
}
