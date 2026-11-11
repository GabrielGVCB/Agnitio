package com.agnitio.application.usecase.event;

import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.event.EventNotFoundException;

import java.util.Objects;

public class GetEventByIdUseCase {
    private final EventRepository eventRepository;

    public GetEventByIdUseCase(EventRepository eventRepository) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
    }

    public Event execute(EventId id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }
}
