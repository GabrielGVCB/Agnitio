package com.agnitio.application.usecase.photo;

import com.agnitio.application.command.RequestUploadUrlsCommand;
import com.agnitio.application.dto.UploadUrlItem;
import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.application.ports.output.PhotoStorage;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventNotFoundException;
import com.agnitio.domain.event.UnauthorizedEventAccessException;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RequestUploadUrlsUseCase {

    private static final Duration UPLOAD_EXPIRATION = Duration.ofMinutes(15);

    private final EventRepository eventRepository;
    private final PhotoRepository photoRepository;
    private final PhotoStorage photoStorage;

    public RequestUploadUrlsUseCase(
            EventRepository eventRepository,
            PhotoRepository photoRepository,
            PhotoStorage photoStorage
    ) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
        this.photoRepository = Objects.requireNonNull(photoRepository, "photoRepository must not be null");
        this.photoStorage = Objects.requireNonNull(photoStorage, "photoStorage must not be null");
    }

    public List<UploadUrlItem> execute(RequestUploadUrlsCommand command) {
        Event event = eventRepository.findById(command.eventId())
                .orElseThrow(() -> new EventNotFoundException(command.eventId()));

        if (!event.getPhotographerId().equals(command.photographerId())) {
            throw new UnauthorizedEventAccessException("Photographer is not the owner of this event");
        }

        List<Photo> photosToSave = new ArrayList<>(command.files().size());
        List<UploadUrlItem> results = new ArrayList<>(command.files().size());

        for (RequestUploadUrlsCommand.FileItem fileItem : command.files()) {
            PhotoId photoId = PhotoId.generate();
            Photo photo = new Photo(photoId, event.getId(), fileItem.fileName(), Instant.now());
            photosToSave.add(photo);

            String storageKey = photo.getOriginalStorageKey();
            String uploadUrl = photoStorage.generatePresignedUploadUrl(storageKey, UPLOAD_EXPIRATION);

            results.add(new UploadUrlItem(photoId, fileItem.fileName(), uploadUrl, storageKey));
        }

        photoRepository.saveAll(photosToSave);
        return results;
    }
}
