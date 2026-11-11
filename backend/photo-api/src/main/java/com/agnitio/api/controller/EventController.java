package com.agnitio.api.controller;

import com.agnitio.api.controller.dto.CreateEventRequestDto;
import com.agnitio.api.controller.dto.EventResponseDto;
import com.agnitio.api.controller.dto.UpdateEventRequestDto;
import com.agnitio.application.command.CreateEventCommand;
import com.agnitio.application.command.UpdateEventCommand;
import com.agnitio.application.usecase.event.CreateEventUseCase;
import com.agnitio.application.usecase.event.DeleteEventUseCase;
import com.agnitio.application.usecase.event.GetEventByIdUseCase;
import com.agnitio.application.usecase.event.ListPhotographerEventsUseCase;
import com.agnitio.application.usecase.event.UpdateEventUseCase;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.event.EventVisibility;
import com.agnitio.domain.event.UnauthorizedEventAccessException;
import com.agnitio.domain.photographer.InvalidCredentialsException;
import com.agnitio.domain.photographer.PhotographerId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final CreateEventUseCase createEventUseCase;
    private final GetEventByIdUseCase getEventByIdUseCase;
    private final ListPhotographerEventsUseCase listPhotographerEventsUseCase;
    private final UpdateEventUseCase updateEventUseCase;
    private final DeleteEventUseCase deleteEventUseCase;

    public EventController(
            CreateEventUseCase createEventUseCase,
            GetEventByIdUseCase getEventByIdUseCase,
            ListPhotographerEventsUseCase listPhotographerEventsUseCase,
            UpdateEventUseCase updateEventUseCase,
            DeleteEventUseCase deleteEventUseCase
    ) {
        this.createEventUseCase = Objects.requireNonNull(createEventUseCase, "createEventUseCase must not be null");
        this.getEventByIdUseCase = Objects.requireNonNull(getEventByIdUseCase, "getEventByIdUseCase must not be null");
        this.listPhotographerEventsUseCase = Objects.requireNonNull(listPhotographerEventsUseCase, "listPhotographerEventsUseCase must not be null");
        this.updateEventUseCase = Objects.requireNonNull(updateEventUseCase, "updateEventUseCase must not be null");
        this.deleteEventUseCase = Objects.requireNonNull(deleteEventUseCase, "deleteEventUseCase must not be null");
    }

    @PostMapping
    public ResponseEntity<EventResponseDto> createEvent(
            @AuthenticationPrincipal String photographerIdStr,
            @RequestBody @Valid CreateEventRequestDto request
    ) {
        PhotographerId photographerId = new PhotographerId(validatePhotographerId(photographerIdStr));
        CreateEventCommand command = new CreateEventCommand(
                photographerId,
                request.name(),
                request.type(),
                request.visibility(),
                request.accessCode(),
                request.biometricRetentionDays()
        );

        Event event = createEventUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponseDto.fromDomain(event));
    }

    @GetMapping
    public ResponseEntity<List<EventResponseDto>> listEvents(
            @AuthenticationPrincipal String photographerIdStr
    ) {
        PhotographerId photographerId = new PhotographerId(validatePhotographerId(photographerIdStr));
        List<Event> events = listPhotographerEventsUseCase.execute(photographerId);
        List<EventResponseDto> dtos = events.stream().map(EventResponseDto::fromDomain).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDto> getEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal String photographerIdStr
    ) {
        PhotographerId photographerId = new PhotographerId(validatePhotographerId(photographerIdStr));
        Event event = getEventByIdUseCase.execute(new EventId(id));

        if (!event.isOwnedBy(photographerId)) {
            throw new UnauthorizedEventAccessException("You do not have permission to view this event");
        }

        return ResponseEntity.ok(EventResponseDto.fromDomain(event));
    }

    @GetMapping("/{id}/public")
    public ResponseEntity<EventResponseDto> getPublicEvent(
            @PathVariable UUID id,
            @RequestParam(required = false) String code
    ) {
        Event event = getEventByIdUseCase.execute(new EventId(id));

        if (event.getVisibility() == EventVisibility.PRIVATE && !event.verifyAccess(code)) {
            throw new UnauthorizedEventAccessException("Invalid access code for private event");
        }

        return ResponseEntity.ok(EventResponseDto.fromDomain(event));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponseDto> updateEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal String photographerIdStr,
            @RequestBody @Valid UpdateEventRequestDto request
    ) {
        PhotographerId photographerId = new PhotographerId(validatePhotographerId(photographerIdStr));
        UpdateEventCommand command = new UpdateEventCommand(
                new EventId(id),
                photographerId,
                request.name(),
                request.type(),
                request.visibility(),
                request.accessCode(),
                request.biometricRetentionDays()
        );

        Event updated = updateEventUseCase.execute(command);
        return ResponseEntity.ok(EventResponseDto.fromDomain(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal String photographerIdStr
    ) {
        PhotographerId photographerId = new PhotographerId(validatePhotographerId(photographerIdStr));
        deleteEventUseCase.execute(new EventId(id), photographerId);
        return ResponseEntity.noContent().build();
    }

    private UUID validatePhotographerId(String photographerIdStr) {
        if (photographerIdStr == null || photographerIdStr.isBlank()) {
            throw new InvalidCredentialsException();
        }
        return UUID.fromString(photographerIdStr);
    }
}
