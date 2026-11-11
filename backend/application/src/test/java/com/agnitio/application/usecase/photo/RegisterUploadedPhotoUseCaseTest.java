package com.agnitio.application.usecase.photo;

import com.agnitio.application.ports.output.EventPublisher;
import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;
import com.agnitio.domain.photo.PhotoStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUploadedPhotoUseCaseTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private RegisterUploadedPhotoUseCase useCase;

    @Test
    @DisplayName("Should successfully mark photo as uploaded, persist it, and publish event")
    void shouldSuccessfullyRegisterUploadedPhoto() {
        PhotoId photoId = PhotoId.generate();
        EventId eventId = EventId.generate();
        Photo photo = new Photo(photoId, eventId, "runner.jpg", Instant.now());

        when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

        Photo result = useCase.execute(photoId);

        assertThat(result.getStatus()).isEqualTo(PhotoStatus.UPLOADED);
        verify(photoRepository).save(photo);
        verify(eventPublisher).publish(eq("photo.uploaded"), any(Photo.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when photo is not found")
    void shouldThrowWhenPhotoNotFound() {
        PhotoId photoId = PhotoId.generate();
        when(photoRepository.findById(photoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(photoId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Photo not found");
    }
}
