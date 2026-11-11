package com.agnitio.domain.event;

import com.agnitio.domain.photographer.PhotographerId;
import org.junit.jupiter.api.Test;

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
                90
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
                30
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
                30
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
                0
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
                60
        );

        event.archive();
        assertThat(event.getStatus()).isEqualTo(EventStatus.ARCHIVED);

        assertThatThrownBy(event::archive)
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("already archived");
    }
}
