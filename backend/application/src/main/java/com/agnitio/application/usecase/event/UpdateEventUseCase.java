package com.agnitio.application.usecase.event;

import com.agnitio.application.command.UpdateEventCommand;
import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventNotFoundException;
import com.agnitio.domain.event.UnauthorizedEventAccessException;

import java.util.Objects;

public class UpdateEventUseCase {
    private final EventRepository eventRepository;

    public UpdateEventUseCase(EventRepository eventRepository) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
    }

    public Event execute(UpdateEventCommand command) {
        Event event = eventRepository.findById(command.eventId())
                .orElseThrow(() -> new EventNotFoundException(command.eventId()));

        if (!event.isOwnedBy(command.photographerId())) {
            throw new UnauthorizedEventAccessException("Photographer is not the owner of this event");
        }

        event.update(
                command.name(),
                command.type(),
                command.visibility(),
                command.accessCode(),
                command.biometricRetentionDays()
        );

        return eventRepository.save(event);
    }
}
