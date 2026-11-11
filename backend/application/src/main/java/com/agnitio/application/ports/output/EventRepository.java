package com.agnitio.application.ports.output;

import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photographer.PhotographerId;

import java.util.List;
import java.util.Optional;

public interface EventRepository {
    Event save(Event event);
    Optional<Event> findById(EventId id);
    List<Event> findByPhotographerId(PhotographerId photographerId);
    boolean existsByIdAndPhotographerId(EventId id, PhotographerId photographerId);
    void deleteById(EventId id);
}
