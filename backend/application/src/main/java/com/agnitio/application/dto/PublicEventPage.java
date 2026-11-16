package com.agnitio.application.dto;

import com.agnitio.domain.event.Event;

import java.util.List;
import java.util.Objects;

/**
 * One page of publicly listed events. Uses "hasNext" instead of a total count so the
 * search does not need an extra COUNT query.
 */
public record PublicEventPage(List<Event> events, int page, int size, boolean hasNext) {
    public PublicEventPage {
        events = List.copyOf(Objects.requireNonNull(events, "events must not be null"));
    }
}
