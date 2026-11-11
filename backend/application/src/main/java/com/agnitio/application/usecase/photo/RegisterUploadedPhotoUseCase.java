package com.agnitio.application.usecase.photo;

import com.agnitio.application.ports.output.EventPublisher;
import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;

import java.util.Objects;

public class RegisterUploadedPhotoUseCase {
    private final PhotoRepository photoRepository;
    private final EventPublisher eventPublisher;

    public RegisterUploadedPhotoUseCase(PhotoRepository photoRepository, EventPublisher eventPublisher) {
        this.photoRepository = Objects.requireNonNull(photoRepository, "photoRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    public Photo execute(PhotoId photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found: " + photoId));

        photo.markAsUploaded();
        photoRepository.save(photo);
        eventPublisher.publish("photo.uploaded", photo);

        return photo;
    }
}
