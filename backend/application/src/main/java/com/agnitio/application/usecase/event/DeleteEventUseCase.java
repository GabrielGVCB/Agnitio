package com.agnitio.application.usecase.event;

import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.event.EventNotFoundException;
import com.agnitio.domain.event.UnauthorizedEventAccessException;
import com.agnitio.domain.photographer.PhotographerId;

import java.util.Objects;

public class DeleteEventUseCase {
    private final EventRepository eventRepository;

    public DeleteEventUseCase(EventRepository eventRepository) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
    }

    public void execute(EventId eventId, PhotographerId photographerId) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(photographerId, "photographerId must not be null");

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (!event.isOwnedBy(photographerId)) {
            throw new UnauthorizedEventAccessException("Photographer is not the owner of this event");
        }

        eventRepository.deleteById(eventId);
    }
}
