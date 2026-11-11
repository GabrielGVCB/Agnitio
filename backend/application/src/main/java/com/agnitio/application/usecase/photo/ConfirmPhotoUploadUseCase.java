package com.agnitio.application.usecase.photo;

import com.agnitio.application.command.ConfirmPhotoUploadCommand;
import com.agnitio.application.ports.output.EventPublisher;
import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.application.ports.output.PhotoProgressTracker;
import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventNotFoundException;
import com.agnitio.domain.event.UnauthorizedEventAccessException;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoStatus;
import com.agnitio.domain.photo.PhotoUploadedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ConfirmPhotoUploadUseCase {

    private final EventRepository eventRepository;
    private final PhotoRepository photoRepository;
    private final EventPublisher eventPublisher;
    private final PhotoProgressTracker photoProgressTracker;

    public ConfirmPhotoUploadUseCase(
            EventRepository eventRepository,
            PhotoRepository photoRepository,
            EventPublisher eventPublisher
    ) {
        this(eventRepository, photoRepository, eventPublisher, null);
    }

    public ConfirmPhotoUploadUseCase(
            EventRepository eventRepository,
            PhotoRepository photoRepository,
            EventPublisher eventPublisher,
            PhotoProgressTracker photoProgressTracker
    ) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
        this.photoRepository = Objects.requireNonNull(photoRepository, "photoRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.photoProgressTracker = photoProgressTracker;
    }

    public List<Photo> execute(ConfirmPhotoUploadCommand command) {
        Event event = eventRepository.findById(command.eventId())
                .orElseThrow(() -> new EventNotFoundException(command.eventId()));

        if (!event.getPhotographerId().equals(command.photographerId())) {
            throw new UnauthorizedEventAccessException("Photographer is not the owner of this event");
        }

        List<Photo> photos = photoRepository.findAllById(command.photoIds());
        List<Photo> updatedPhotos = new ArrayList<>();
        UUID correlationId = UUID.randomUUID();
        int newlyUploadedCount = 0;

        for (Photo photo : photos) {
            if (!photo.getEventId().equals(event.getId())) {
                continue;
            }

            if (photo.getStatus() == PhotoStatus.PENDING_UPLOAD) {
                photo.markAsUploaded();
                updatedPhotos.add(photo);
                eventPublisher.publish("photo.uploaded", PhotoUploadedEvent.of(photo, correlationId));
                newlyUploadedCount++;
            } else {
                updatedPhotos.add(photo);
            }
        }

        if (photoProgressTracker != null && newlyUploadedCount > 0) {
            photoProgressTracker.initProgress(event.getId().value(), newlyUploadedCount);
        }

        photoRepository.saveAll(updatedPhotos);
        return updatedPhotos;
    }
}
