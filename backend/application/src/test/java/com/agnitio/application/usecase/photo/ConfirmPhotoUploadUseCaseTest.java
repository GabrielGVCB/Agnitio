package com.agnitio.application.usecase.photo;

import com.agnitio.application.command.ConfirmPhotoUploadCommand;
import com.agnitio.application.ports.output.EventPublisher;
import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.event.EventNotFoundException;
import com.agnitio.domain.event.EventType;
import com.agnitio.domain.event.EventVisibility;
import com.agnitio.domain.event.UnauthorizedEventAccessException;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;
import com.agnitio.domain.photo.PhotoStatus;
import com.agnitio.domain.photographer.PhotographerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmPhotoUploadUseCaseTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ConfirmPhotoUploadUseCase useCase;

    @Test
    @DisplayName("Should successfully mark photos as uploaded and publish photo.uploaded events")
    void shouldSuccessfullyConfirmUploads() {
        PhotographerId photographerId = PhotographerId.generate();
        EventId eventId = EventId.generate();
        Event event = Event.create(eventId, photographerId, "Marathon", EventType.RACE, EventVisibility.PUBLIC, null, 30, null, null);

        PhotoId photoId1 = PhotoId.generate();
        PhotoId photoId2 = PhotoId.generate();
        Photo photo1 = new Photo(photoId1, eventId, "run1.jpg", Instant.now());
        Photo photo2 = new Photo(photoId2, eventId, "run2.jpg", Instant.now());

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(photoRepository.findAllById(List.of(photoId1, photoId2))).thenReturn(List.of(photo1, photo2));

        ConfirmPhotoUploadCommand command = new ConfirmPhotoUploadCommand(eventId, photographerId, List.of(photoId1, photoId2));
        List<Photo> result = useCase.execute(command);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(PhotoStatus.UPLOADED);
        assertThat(result.get(1).getStatus()).isEqualTo(PhotoStatus.UPLOADED);

        verify(photoRepository).saveAll(any());
        verify(eventPublisher, times(2)).publish(eq("photo.uploaded"), any());
    }

    @Test
    @DisplayName("Should be idempotent when confirming photos that are already uploaded")
    void shouldBeIdempotentForAlreadyUploadedPhotos() {
        PhotographerId photographerId = PhotographerId.generate();
        EventId eventId = EventId.generate();
        Event event = Event.create(eventId, photographerId, "Marathon", EventType.RACE, EventVisibility.PUBLIC, null, 30, null, null);

        PhotoId photoId1 = PhotoId.generate();
        Photo photo1 = new Photo(photoId1, eventId, "run1.jpg", Instant.now(), PhotoStatus.UPLOADED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(photoRepository.findAllById(List.of(photoId1))).thenReturn(List.of(photo1));

        ConfirmPhotoUploadCommand command = new ConfirmPhotoUploadCommand(eventId, photographerId, List.of(photoId1));
        List<Photo> result = useCase.execute(command);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PhotoStatus.UPLOADED);
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedEventAccessException if photographer is not event owner")
    void shouldThrowWhenNotOwner() {
        PhotographerId ownerId = PhotographerId.generate();
        PhotographerId intruderId = PhotographerId.generate();
        EventId eventId = EventId.generate();
        Event event = Event.create(eventId, ownerId, "Marathon", EventType.RACE, EventVisibility.PUBLIC, null, 30, null, null);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        ConfirmPhotoUploadCommand command = new ConfirmPhotoUploadCommand(eventId, intruderId, List.of(PhotoId.generate()));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(UnauthorizedEventAccessException.class);
    }
}
