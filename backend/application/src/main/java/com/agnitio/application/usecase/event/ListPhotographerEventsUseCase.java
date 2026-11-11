package com.agnitio.application.usecase.event;

import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.photographer.PhotographerId;

import java.util.List;
import java.util.Objects;

public class ListPhotographerEventsUseCase {
    private final EventRepository eventRepository;

    public ListPhotographerEventsUseCase(EventRepository eventRepository) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
    }

    public List<Event> execute(PhotographerId photographerId) {
        Objects.requireNonNull(photographerId, "photographerId must not be null");
        return eventRepository.findByPhotographerId(photographerId);
    }
}
