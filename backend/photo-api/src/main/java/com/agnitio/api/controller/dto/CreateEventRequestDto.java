package com.agnitio.api.controller.dto;

import com.agnitio.domain.event.EventType;
import com.agnitio.domain.event.EventVisibility;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEventRequestDto(
        @NotBlank(message = "Event name is required")
        @Size(max = 150, message = "Event name must be at most 150 characters")
        String name,

        @NotNull(message = "Event type is required")
        EventType type,

        @NotNull(message = "Event visibility is required")
        EventVisibility visibility,

        String accessCode,

        @Min(value = 1, message = "Biometric retention days must be at least 1")
        int biometricRetentionDays
) {
}
