package com.agnitio.application.usecase.photo;

import com.agnitio.application.command.ProcessPhotoCommand;
import com.agnitio.application.dto.ProcessedVariants;
import com.agnitio.application.ports.output.EventPublisher;
import com.agnitio.application.ports.output.ImageProcessor;
import com.agnitio.application.ports.output.PhotoProgressTracker;
import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.application.ports.output.PhotoStorage;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;
import com.agnitio.domain.photo.PhotoProcessedEvent;
import com.agnitio.domain.photo.PhotoStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessPhotoUseCaseTest {

    private PhotoRepository photoRepository;
    private PhotoStorage photoStorage;
    private ImageProcessor imageProcessor;
    private EventPublisher eventPublisher;
    private PhotoProgressTracker photoProgressTracker;
    private ProcessPhotoUseCase useCase;

    @BeforeEach
    void setUp() {
        photoRepository = mock(PhotoRepository.class);
        photoStorage = mock(PhotoStorage.class);
        imageProcessor = mock(ImageProcessor.class);
        eventPublisher = mock(EventPublisher.class);
        photoProgressTracker = mock(PhotoProgressTracker.class);

        useCase = new ProcessPhotoUseCase(
                photoRepository,
                photoStorage,
                imageProcessor,
                eventPublisher,
                photoProgressTracker
        );
    }

    @Test
    @DisplayName("Should successfully process uploaded photo, upload variants, set status to READY and publish event")
    void shouldProcessUploadedPhotoSuccessfully() {
        PhotoId photoId = new PhotoId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());
        Photo photo = new Photo(photoId, eventId, "runner1.jpg", Instant.now(), PhotoStatus.UPLOADED);
        UUID correlationId = UUID.randomUUID();

        when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

        byte[] fakeOriginal = "original-image-bytes".getBytes();
        byte[] fakeThumb = "thumb-webp".getBytes();
        byte[] fakePreview = "preview-webp".getBytes();

        when(photoStorage.download(photo.getOriginalStorageKey())).thenReturn(fakeOriginal);
        when(imageProcessor.process(fakeOriginal)).thenReturn(
                new ProcessedVariants(fakeThumb, fakePreview, 1920, 1080)
        );

        ProcessPhotoCommand command = new ProcessPhotoCommand(photoId.value(), correlationId);
        useCase.execute(command);

        assertThat(photo.getStatus()).isEqualTo(PhotoStatus.READY);

        verify(photoStorage).upload(eq(photo.getThumbnailStorageKey()), eq(fakeThumb), eq("image/webp"));
        verify(photoStorage).upload(eq(photo.getPreviewStorageKey()), eq(fakePreview), eq("image/webp"));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eq("photo.processed"), eventCaptor.capture());

        PhotoProcessedEvent published = (PhotoProcessedEvent) eventCaptor.getValue();
        assertThat(published.photoId()).isEqualTo(photoId.value());
        assertThat(published.eventId()).isEqualTo(eventId.value());
        assertThat(published.width()).isEqualTo(1920);
        assertThat(published.height()).isEqualTo(1080);
        assertThat(published.thumbnailStorageKey()).isEqualTo(photo.getThumbnailStorageKey());
        assertThat(published.previewStorageKey()).isEqualTo(photo.getPreviewStorageKey());

        verify(photoProgressTracker).incrementProcessed(eventId.value());
    }

    @Test
    @DisplayName("Should be idempotent and do nothing if photo is already READY")
    void shouldBeIdempotentWhenPhotoAlreadyReady() {
        PhotoId photoId = new PhotoId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());
        Photo photo = new Photo(photoId, eventId, "runner1.jpg", Instant.now(), PhotoStatus.READY);
        UUID correlationId = UUID.randomUUID();

        when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

        ProcessPhotoCommand command = new ProcessPhotoCommand(photoId.value(), correlationId);
        useCase.execute(command);

        verify(photoStorage, never()).download(anyString());
        verify(imageProcessor, never()).process(any());
        verify(eventPublisher, never()).publish(anyString(), any());
        verify(photoProgressTracker, never()).incrementProcessed(any());
    }

    @Test
    @DisplayName("Should mark photo as FAILED and increment failed progress when processing throws exception")
    void shouldMarkAsFailedWhenProcessingFails() {
        PhotoId photoId = new PhotoId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());
        Photo photo = new Photo(photoId, eventId, "corrupt.jpg", Instant.now(), PhotoStatus.UPLOADED);
        UUID correlationId = UUID.randomUUID();

        when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));
        when(photoStorage.download(photo.getOriginalStorageKey())).thenThrow(new RuntimeException("S3 read error"));

        ProcessPhotoCommand command = new ProcessPhotoCommand(photoId.value(), correlationId);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process photo");

        assertThat(photo.getStatus()).isEqualTo(PhotoStatus.FAILED);
        verify(photoProgressTracker).incrementFailed(eventId.value());
        verify(eventPublisher, never()).publish(anyString(), any());
    }
}
