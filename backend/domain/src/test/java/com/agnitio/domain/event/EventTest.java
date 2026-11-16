package com.agnitio.domain.event;

import com.agnitio.domain.photographer.PhotographerId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTest {

    @Test
    void shouldCreatePublicEventSuccessfully() {
        EventId eventId = EventId.generate();
        PhotographerId photographerId = PhotographerId.generate();

        Event event = Event.create(
                eventId,
                photographerId,
                "City Marathon 2026",
                EventType.RACE,
                EventVisibility.PUBLIC,
                null,
                90,
                null,
                null
        );

        assertThat(event.getId()).isEqualTo(eventId);
        assertThat(event.getPhotographerId()).isEqualTo(photographerId);
        assertThat(event.getName()).isEqualTo("City Marathon 2026");
        assertThat(event.getType()).isEqualTo(EventType.RACE);
        assertThat(event.getVisibility()).isEqualTo(EventVisibility.PUBLIC);
        assertThat(event.getAccessCode()).isNull();
        assertThat(event.getBiometricRetentionDays()).isEqualTo(90);
        assertThat(event.getStatus()).isEqualTo(EventStatus.ACTIVE);
        assertThat(event.verifyAccess("any-code")).isTrue();
    }

    @Test
    void shouldCreatePrivateEventAndVerifyAccessCode() {
        EventId eventId = EventId.generate();
        PhotographerId photographerId = PhotographerId.generate();

        Event event = Event.create(
                eventId,
                photographerId,
                "VIP Wedding",
                EventType.WEDDING,
                EventVisibility.PRIVATE,
                "SECRET123",
                30,
                null,
                null
        );

        assertThat(event.getVisibility()).isEqualTo(EventVisibility.PRIVATE);
        assertThat(event.getAccessCode()).isEqualTo("SECRET123");
        assertThat(event.verifyAccess("SECRET123")).isTrue();
        assertThat(event.verifyAccess("WRONG")).isFalse();
        assertThat(event.verifyAccess(null)).isFalse();
    }

    @Test
    void shouldFailWhenPrivateEventHasNoAccessCode() {
        EventId eventId = EventId.generate();
        PhotographerId photographerId = PhotographerId.generate();

        assertThatThrownBy(() -> Event.create(
                eventId,
                photographerId,
                "VIP Wedding",
                EventType.WEDDING,
                EventVisibility.PRIVATE,
                "  ",
                30,
                null,
                null
        )).isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("Access code is required for private events");
    }

    @Test
    void shouldFailWhenRetentionDaysIsLessThanOne() {
        EventId eventId = EventId.generate();
        PhotographerId photographerId = PhotographerId.generate();

        assertThatThrownBy(() -> Event.create(
                eventId,
                photographerId,
                "Festival",
                EventType.OTHER,
                EventVisibility.PUBLIC,
                null,
                0,
                null,
                null
        )).isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("Biometric retention days must be at least 1");
    }

    @Test
    void shouldArchiveEvent() {
        Event event = Event.create(
                EventId.generate(),
                PhotographerId.generate(),
                "Graduation 2026",
                EventType.GRADUATION,
                EventVisibility.PUBLIC,
                null,
                60,
                null,
                null
        );

        event.archive();
        assertThat(event.getStatus()).isEqualTo(EventStatus.ARCHIVED);

        assertThatThrownBy(event::archive)
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("already archived");
    }
    @Test
    void shouldStoreEventDateAndTrimmedCity() {
        LocalDate date = LocalDate.of(2026, 5, 12);

        Event event = Event.create(
                EventId.generate(),
                PhotographerId.generate(),
                "City Marathon 2026",
                EventType.RACE,
                EventVisibility.PUBLIC,
                null,
                90,
                date,
                "  Rio de Janeiro, RJ  "
        );

        assertThat(event.getEventDate()).isEqualTo(date);
        assertThat(event.getCity()).isEqualTo("Rio de Janeiro, RJ");
    }

    @Test
    void shouldNormalizeBlankCityToNull() {
        Event event = Event.create(
                EventId.generate(),
                PhotographerId.generate(),
                "Festival",
                EventType.OTHER,
                EventVisibility.PUBLIC,
                null,
                30,
                null,
                "   "
        );

        assertThat(event.getCity()).isNull();
        assertThat(event.getEventDate()).isNull();
    }

    @Test
    void shouldFailWhenCityIsTooLong() {
        String longCity = "x".repeat(Event.CITY_MAX_LENGTH + 1);

        assertThatThrownBy(() -> Event.create(
                EventId.generate(),
                PhotographerId.generate(),
                "Festival",
                EventType.OTHER,
                EventVisibility.PUBLIC,
                null,
                30,
                null,
                longCity
        )).isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("City must be at most");
    }

    @Test
    void shouldUpdateEventDateAndCity() {
        Event event = Event.create(
                EventId.generate(),
                PhotographerId.generate(),
                "Festival",
                EventType.OTHER,
                EventVisibility.PUBLIC,
                null,
                30,
                null,
                null
        );

        event.update("Festival 2026", EventType.OTHER, EventVisibility.PUBLIC, null, 30,
                LocalDate.of(2026, 8, 1), "Curitiba, PR");

        assertThat(event.getEventDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(event.getCity()).isEqualTo("Curitiba, PR");
    }

    @Test
    void shouldOnlyListActivePublicEvents() {
        Event publicEvent = Event.create(EventId.generate(), PhotographerId.generate(), "Open Run",
                EventType.RACE, EventVisibility.PUBLIC, null, 30, null, null);
        Event privateEvent = Event.create(EventId.generate(), PhotographerId.generate(), "Wedding",
                EventType.WEDDING, EventVisibility.PRIVATE, "CODE", 30, null, null);
        Event archivedEvent = Event.create(EventId.generate(), PhotographerId.generate(), "Old Run",
                EventType.RACE, EventVisibility.PUBLIC, null, 30, null, null);
        archivedEvent.archive();

        assertThat(publicEvent.isPubliclyListed()).isTrue();
        assertThat(privateEvent.isPubliclyListed()).isFalse();
        assertThat(archivedEvent.isPubliclyListed()).isFalse();
    }
}
