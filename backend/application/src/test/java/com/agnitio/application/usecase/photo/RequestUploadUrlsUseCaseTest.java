package com.agnitio.application.usecase.photo;

import com.agnitio.application.command.RequestUploadUrlsCommand;
import com.agnitio.application.dto.UploadUrlItem;
import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.application.ports.output.PhotoRepository;
import com.agnitio.application.ports.output.PhotoStorage;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.event.EventNotFoundException;
import com.agnitio.domain.event.EventType;
import com.agnitio.domain.event.EventVisibility;
import com.agnitio.domain.event.UnauthorizedEventAccessException;
import com.agnitio.domain.photographer.PhotographerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestUploadUrlsUseCaseTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private PhotoStorage photoStorage;

    @InjectMocks
    private RequestUploadUrlsUseCase useCase;

    @Test
    @DisplayName("Should successfully generate presigned upload URLs and save pending photos")
    void shouldSuccessfullyGeneratePresignedUrls() {
        PhotographerId photographerId = PhotographerId.generate();
        EventId eventId = EventId.generate();
        Event event = Event.create(eventId, photographerId, "10K Run", EventType.RACE, EventVisibility.PUBLIC, null, 30);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(photoStorage.generatePresignedUploadUrl(any(), any(Duration.class)))
                .thenReturn("https://minio.local/presigned-url");

        RequestUploadUrlsCommand command = new RequestUploadUrlsCommand(
                eventId,
                photographerId,
                List.of(
                        new RequestUploadUrlsCommand.FileItem("photo1.jpg"),
                        new RequestUploadUrlsCommand.FileItem("photo2.png")
                )
        );

        List<UploadUrlItem> result = useCase.execute(command);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).fileName()).isEqualTo("photo1.jpg");
        assertThat(result.get(0).uploadUrl()).isEqualTo("https://minio.local/presigned-url");
        assertThat(result.get(0).storageKey()).contains("events/" + eventId.value() + "/original/");

        verify(photoRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedEventAccessException if photographer is not the event owner")
    void shouldThrowWhenPhotographerIsNotOwner() {
        PhotographerId ownerId = PhotographerId.generate();
        PhotographerId intruderId = PhotographerId.generate();
        EventId eventId = EventId.generate();
        Event event = Event.create(eventId, ownerId, "10K Run", EventType.RACE, EventVisibility.PUBLIC, null, 30);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        RequestUploadUrlsCommand command = new RequestUploadUrlsCommand(
                eventId,
                intruderId,
                List.of(new RequestUploadUrlsCommand.FileItem("photo.jpg"))
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(UnauthorizedEventAccessException.class)
                .hasMessageContaining("not the owner");
    }

    @Test
    @DisplayName("Should throw EventNotFoundException if event does not exist")
    void shouldThrowWhenEventNotFound() {
        PhotographerId photographerId = PhotographerId.generate();
        EventId eventId = EventId.generate();

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        RequestUploadUrlsCommand command = new RequestUploadUrlsCommand(
                eventId,
                photographerId,
                List.of(new RequestUploadUrlsCommand.FileItem("photo.jpg"))
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining("Event not found");
    }
}
