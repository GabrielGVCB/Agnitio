package com.agnitio.application.usecase.photo;

import com.agnitio.application.command.SearchPhotosBySelfieCommand;
import com.agnitio.application.dto.FaceMatchDto;
import com.agnitio.application.dto.SelfieSearchResultDto;
import com.agnitio.application.ports.output.FaceSearchClient;
import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.application.ports.output.PhotoStorage;
import com.agnitio.application.ports.output.RateLimiterPort;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photo.InvalidBiometricConsentException;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;
import com.agnitio.domain.photo.PhotoStatus;
import com.agnitio.domain.photo.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SearchPhotosBySelfieUseCaseTest {

    private FaceSearchClient faceSearchClient;
    private PhotoRepository photoRepository;
    private PhotoStorage photoStorage;
    private RateLimiterPort rateLimiter;
    private SearchPhotosBySelfieUseCase useCase;

    @BeforeEach
    void setUp() {
        faceSearchClient = mock(FaceSearchClient.class);
        photoRepository = mock(PhotoRepository.class);
        photoStorage = mock(PhotoStorage.class);
        rateLimiter = mock(RateLimiterPort.class);

        useCase = new SearchPhotosBySelfieUseCase(
                faceSearchClient,
                photoRepository,
                photoStorage,
                rateLimiter
        );
    }

    @Test
    @DisplayName("should throw InvalidBiometricConsentException when consent is not given")
    void shouldThrowWhenConsentNotGiven() {
        SearchPhotosBySelfieCommand command = new SearchPhotosBySelfieCommand(
                UUID.randomUUID(),
                new byte[]{1, 2, 3},
                "selfie.jpg",
                false,
                "127.0.0.1"
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidBiometricConsentException.class)
                .hasMessageContaining("Explicit biometric consent is required");

        verifyNoInteractions(faceSearchClient, photoRepository, photoStorage);
    }

    @Test
    @DisplayName("should throw RateLimitExceededException when rate limit is exceeded")
    void shouldThrowWhenRateLimitExceeded() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(false);

        SearchPhotosBySelfieCommand command = new SearchPhotosBySelfieCommand(
                UUID.randomUUID(),
                new byte[]{1, 2, 3},
                "selfie.jpg",
                true,
                "192.168.1.1"
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Too many selfie searches");

        verifyNoInteractions(faceSearchClient, photoRepository, photoStorage);
    }

    @Test
    @DisplayName("should search faces and return matched photos with pre-signed URLs")
    void shouldSearchFacesSuccessfully() {
        UUID eventId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);

        when(faceSearchClient.searchFacesBySelfie(eq(eventId), any(), anyString(), anyDouble(), anyInt()))
                .thenReturn(List.of(new FaceMatchDto(photoId, 0.95)));

        Photo photo = new Photo(
                new PhotoId(photoId),
                new EventId(eventId),
                "photo.jpg",
                java.time.Instant.now()
        );
        photo.markAsUploaded();
        photo.startProcessing();
        photo.completeProcessing();
        assertThat(photo.getStatus()).isEqualTo(PhotoStatus.READY);

        String previewKey = "events/" + eventId + "/preview/" + photoId + ".webp";
        String thumbKey = "events/" + eventId + "/thumb/" + photoId + ".webp";

        when(photoRepository.findById(new PhotoId(photoId))).thenReturn(Optional.of(photo));
        when(photoStorage.generatePresignedDownloadUrl(eq(previewKey), any(Duration.class)))
                .thenReturn("https://storage.agnitio.com/preview/signed.webp");
        when(photoStorage.generatePresignedDownloadUrl(eq(thumbKey), any(Duration.class)))
                .thenReturn("https://storage.agnitio.com/thumb/signed.webp");

        SearchPhotosBySelfieCommand command = new SearchPhotosBySelfieCommand(
                eventId,
                new byte[]{1, 2, 3},
                "selfie.jpg",
                true,
                "127.0.0.1"
        );

        List<SelfieSearchResultDto> results = useCase.execute(command);

        assertThat(results).hasSize(1);
        SelfieSearchResultDto item = results.get(0);
        assertThat(item.photoId()).isEqualTo(photoId);
        assertThat(item.similarity()).isEqualTo(0.95);
        assertThat(item.previewUrl()).isEqualTo("https://storage.agnitio.com/preview/signed.webp");
        assertThat(item.thumbnailUrl()).isEqualTo("https://storage.agnitio.com/thumb/signed.webp");

        verify(faceSearchClient).searchFacesBySelfie(eq(eventId), any(), eq("selfie.jpg"), anyDouble(), anyInt());
    }
}
