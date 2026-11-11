package com.agnitio.application.command;

import com.agnitio.domain.event.EventType;
import com.agnitio.domain.event.EventVisibility;
import com.agnitio.domain.photographer.PhotographerId;

import java.util.Objects;

public record CreateEventCommand(
        PhotographerId photographerId,
        String name,
        EventType type,
        EventVisibility visibility,
        String accessCode,
        int biometricRetentionDays
) {
    public CreateEventCommand {
        Objects.requireNonNull(photographerId, "photographerId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(visibility, "visibility must not be null");
    }
}
