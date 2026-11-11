package com.agnitio.application.usecase.photo;

import com.agnitio.application.dto.EventGalleryItem;
import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.application.ports.output.PhotoStorage;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.event.EventNotFoundException;
import com.agnitio.domain.event.UnauthorizedEventAccessException;
import com.agnitio.domain.photo.Photo;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class ListEventPhotosUseCase {

    private static final Duration DOWNLOAD_EXPIRATION = Duration.ofHours(1);

    private final EventRepository eventRepository;
    private final PhotoRepository photoRepository;
    private final PhotoStorage photoStorage;

    public ListEventPhotosUseCase(
            EventRepository eventRepository,
            PhotoRepository photoRepository,
            PhotoStorage photoStorage
    ) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
        this.photoRepository = Objects.requireNonNull(photoRepository, "photoRepository must not be null");
        this.photoStorage = Objects.requireNonNull(photoStorage, "photoStorage must not be null");
    }

    public List<EventGalleryItem> execute(EventId eventId, String accessCode) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (!event.verifyAccess(accessCode)) {
            throw new UnauthorizedEventAccessException("Invalid or missing access code for private event");
        }

        List<Photo> photos = photoRepository.findByEventId(eventId);

        return photos.stream()
                .map(photo -> {
                    String originalUrl = photoStorage.generatePresignedDownloadUrl(photo.getOriginalStorageKey(), DOWNLOAD_EXPIRATION);
                    String previewUrl = photoStorage.generatePresignedDownloadUrl(photo.getPreviewStorageKey(), DOWNLOAD_EXPIRATION);
                    String thumbUrl = photoStorage.generatePresignedDownloadUrl(photo.getThumbnailStorageKey(), DOWNLOAD_EXPIRATION);

                    return new EventGalleryItem(
                            photo.getId(),
                            photo.getOriginalFileName(),
                            photo.getStatus(),
                            originalUrl,
                            previewUrl,
                            thumbUrl,
                            photo.getCreatedAt()
                    );
                })
                .toList();
    }
}
