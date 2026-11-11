package com.agnitio.application.usecase.photo;

import com.agnitio.application.command.ProcessPhotoCommand;
import com.agnitio.application.dto.ProcessedVariants;
import com.agnitio.application.ports.output.EventPublisher;
import com.agnitio.application.ports.output.ImageProcessor;
import com.agnitio.application.ports.output.PhotoProgressTracker;
import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.application.ports.output.PhotoStorage;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;
import com.agnitio.domain.photo.PhotoProcessedEvent;
import com.agnitio.domain.photo.PhotoStatus;

import java.util.Objects;

public class ProcessPhotoUseCase {

    private final PhotoRepository photoRepository;
    private final PhotoStorage photoStorage;
    private final ImageProcessor imageProcessor;
    private final EventPublisher eventPublisher;
    private final PhotoProgressTracker photoProgressTracker;

    public ProcessPhotoUseCase(
            PhotoRepository photoRepository,
            PhotoStorage photoStorage,
            ImageProcessor imageProcessor,
            EventPublisher eventPublisher,
            PhotoProgressTracker photoProgressTracker
    ) {
        this.photoRepository = Objects.requireNonNull(photoRepository, "photoRepository must not be null");
        this.photoStorage = Objects.requireNonNull(photoStorage, "photoStorage must not be null");
        this.imageProcessor = Objects.requireNonNull(imageProcessor, "imageProcessor must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.photoProgressTracker = photoProgressTracker;
    }

    public ProcessPhotoUseCase(
            PhotoRepository photoRepository,
            PhotoStorage photoStorage,
            ImageProcessor imageProcessor,
            EventPublisher eventPublisher
    ) {
        this(photoRepository, photoStorage, imageProcessor, eventPublisher, null);
    }

    public void execute(ProcessPhotoCommand command) {
        PhotoId photoId = new PhotoId(command.photoId());
        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null) {
            return;
        }

        // Idempotency: if already processed, do not re-process
        if (photo.getStatus() == PhotoStatus.READY) {
            return;
        }

        try {
            if (photo.getStatus() == PhotoStatus.UPLOADED) {
                photo.startProcessing();
                photoRepository.save(photo);
            }

            byte[] originalBytes = photoStorage.download(photo.getOriginalStorageKey());
            ProcessedVariants variants = imageProcessor.process(originalBytes);

            photoStorage.upload(photo.getThumbnailStorageKey(), variants.thumbnailWebp(), "image/webp");
            photoStorage.upload(photo.getPreviewStorageKey(), variants.previewWebp(), "image/webp");

            photo.completeProcessing();
            photoRepository.save(photo);

            PhotoProcessedEvent processedEvent = PhotoProcessedEvent.of(
                    photo,
                    command.correlationId(),
                    variants.width(),
                    variants.height()
            );
            eventPublisher.publish("photo.processed", processedEvent);

            if (photoProgressTracker != null) {
                photoProgressTracker.incrementProcessed(photo.getEventId().value());
            }
        } catch (Exception e) {
            try {
                photo.markAsFailed();
                photoRepository.save(photo);
            } catch (Exception ignored) {
            }
            if (photoProgressTracker != null) {
                photoProgressTracker.incrementFailed(photo.getEventId().value());
            }
            throw new RuntimeException("Failed to process photo " + command.photoId() + ": " + e.getMessage(), e);
        }
    }
}
