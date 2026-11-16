package com.agnitio.application.usecase.event;

import com.agnitio.application.command.UpdateEventCommand;
import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.event.EventType;
import com.agnitio.domain.event.EventVisibility;
import com.agnitio.domain.event.UnauthorizedEventAccessException;
import com.agnitio.domain.photographer.PhotographerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateEventUseCaseTest {

    @Mock
    private EventRepository eventRepository;

    private UpdateEventUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateEventUseCase(eventRepository);
    }

    @Test
    void shouldUpdateEventSuccessfully() {
        EventId eventId = EventId.generate();
        PhotographerId photographerId = PhotographerId.generate();

        Event event = Event.create(
                eventId,
                photographerId,
                "Old Name",
                EventType.OTHER,
                EventVisibility.PUBLIC,
                null,
                30,
                null,
                null
        );

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEventCommand command = new UpdateEventCommand(
                eventId,
                photographerId,
                "New Name",
                EventType.CORPORATE,
                EventVisibility.PRIVATE,
                "CODE456",
                45,
                null,
                null
        );

        Event updated = useCase.execute(command);

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getType()).isEqualTo(EventType.CORPORATE);
        assertThat(updated.getVisibility()).isEqualTo(EventVisibility.PRIVATE);
        assertThat(updated.getAccessCode()).isEqualTo("CODE456");
        assertThat(updated.getBiometricRetentionDays()).isEqualTo(45);
        verify(eventRepository).save(event);
    }

    @Test
    void shouldFailWhenNotOwner() {
        EventId eventId = EventId.generate();
        PhotographerId ownerId = PhotographerId.generate();
        PhotographerId strangerId = PhotographerId.generate();

        Event event = Event.create(
                eventId,
                ownerId,
                "Old Name",
                EventType.OTHER,
                EventVisibility.PUBLIC,
                null,
                30,
                null,
                null
        );

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        UpdateEventCommand command = new UpdateEventCommand(
                eventId,
                strangerId,
                "Hacked Name",
                EventType.OTHER,
                EventVisibility.PUBLIC,
                null,
                30,
                null,
                null
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(UnauthorizedEventAccessException.class);
    }
}
