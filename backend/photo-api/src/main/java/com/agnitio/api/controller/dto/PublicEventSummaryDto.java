package com.agnitio.api.controller.dto;

import com.agnitio.domain.event.Event;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Event data safe to expose to anonymous participants: no owner id, access code,
 * retention policy or status.
 */
public record PublicEventSummaryDto(
        UUID id,
        String name,
        String type,
        LocalDate eventDate,
        String city
) {
    public static PublicEventSummaryDto fromDomain(Event event) {
        return new PublicEventSummaryDto(
                event.getId().value(),
                event.getName(),
                event.getType().name(),
                event.getEventDate(),
                event.getCity()
        );
    }
}
