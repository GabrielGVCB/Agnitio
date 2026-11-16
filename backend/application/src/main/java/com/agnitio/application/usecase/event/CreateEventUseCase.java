package com.agnitio.application.usecase.event;

import com.agnitio.application.command.CreateEventCommand;
import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.application.ports.output.PhotographerRepository;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photographer.Photographer;
import com.agnitio.domain.photographer.PhotographerNotFoundException;

import java.util.Objects;

public class CreateEventUseCase {
    private final EventRepository eventRepository;
    private final PhotographerRepository photographerRepository;

    public CreateEventUseCase(
            EventRepository eventRepository,
            PhotographerRepository photographerRepository
    ) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
        this.photographerRepository = Objects.requireNonNull(photographerRepository, "photographerRepository must not be null");
    }

    public Event execute(CreateEventCommand command) {
        Photographer photographer = photographerRepository.findById(command.photographerId())
                .orElseThrow(() -> new PhotographerNotFoundException(command.photographerId()));

        if (!photographer.isActive()) {
            throw new PhotographerNotFoundException(command.photographerId());
        }

        Event event = Event.create(
                EventId.generate(),
                command.photographerId(),
                command.name(),
                command.type(),
                command.visibility(),
                command.accessCode(),
                command.biometricRetentionDays(),
                command.eventDate(),
                command.city()
        );

        return eventRepository.save(event);
    }
}
