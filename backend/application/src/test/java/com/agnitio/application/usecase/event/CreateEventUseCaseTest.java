package com.agnitio.application.usecase.event;

import com.agnitio.application.command.CreateEventCommand;
import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.application.ports.output.PhotographerRepository;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventType;
import com.agnitio.domain.event.EventVisibility;
import com.agnitio.domain.photographer.Photographer;
import com.agnitio.domain.photographer.PhotographerId;
import com.agnitio.domain.photographer.PhotographerNotFoundException;
import com.agnitio.domain.photographer.PhotographerRole;
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
class CreateEventUseCaseTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PhotographerRepository photographerRepository;

    private CreateEventUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateEventUseCase(eventRepository, photographerRepository);
    }

    @Test
    void shouldCreateEventSuccessfully() {
        PhotographerId photographerId = PhotographerId.generate();
        Photographer photographer = Photographer.create(
                photographerId,
                "gabriel@example.com",
                "hash",
                "Gabriel",
                PhotographerRole.PHOTOGRAPHER
        );

        when(photographerRepository.findById(photographerId)).thenReturn(Optional.of(photographer));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateEventCommand command = new CreateEventCommand(
                photographerId,
                "Mountain Race 2026",
                EventType.RACE,
                EventVisibility.PUBLIC,
                null,
                60
        );

        Event event = useCase.execute(command);

        assertThat(event).isNotNull();
        assertThat(event.getName()).isEqualTo("Mountain Race 2026");
        assertThat(event.getPhotographerId()).isEqualTo(photographerId);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void shouldFailWhenPhotographerDoesNotExist() {
        PhotographerId photographerId = PhotographerId.generate();
        when(photographerRepository.findById(photographerId)).thenReturn(Optional.empty());

        CreateEventCommand command = new CreateEventCommand(
                photographerId,
                "Mountain Race 2026",
                EventType.RACE,
                EventVisibility.PUBLIC,
                null,
                60
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(PhotographerNotFoundException.class);
    }
}
