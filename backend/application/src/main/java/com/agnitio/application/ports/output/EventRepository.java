package com.agnitio.application.ports.output;

import com.agnitio.application.dto.PublicEventPage;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photographer.PhotographerId;

import java.util.List;
import java.util.Optional;

public interface EventRepository {
    Event save(Event event);
    Optional<Event> findById(EventId id);
    List<Event> findByPhotographerId(PhotographerId photographerId);
    /**
     * Searches events that participants may discover (public and active), matching the
     * query against name or city, case-insensitively. A null query lists all of them.
     * Ordered by event date (most recent first, undated last), then creation date.
     */
    PublicEventPage searchPublic(String query, int page, int size);

    boolean existsByIdAndPhotographerId(EventId id, PhotographerId photographerId);
    void deleteById(EventId id);
}
