package com.agnitio.infrastructure.persistence.event;

import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.event.EventType;
import com.agnitio.domain.event.EventVisibility;
import com.agnitio.domain.photographer.PhotographerId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EventRepositoryAdapter.class)
@Testcontainers
class EventRepositoryAdapterIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("test_events")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private EventRepositoryAdapter adapter;

    @Test
    void shouldPersistAndManageEvents() {
        EventId eventId = EventId.generate();
        PhotographerId photographerId = PhotographerId.generate();

        Event event = Event.create(
                eventId,
                photographerId,
                "São Paulo City Marathon",
                EventType.RACE,
                EventVisibility.PUBLIC,
                null,
                90
        );

        adapter.save(event);

        Optional<Event> found = adapter.findById(eventId);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("São Paulo City Marathon");
        assertThat(found.get().getType()).isEqualTo(EventType.RACE);
        assertThat(found.get().getPhotographerId()).isEqualTo(photographerId);

        List<Event> list = adapter.findByPhotographerId(photographerId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(eventId);

        assertThat(adapter.existsByIdAndPhotographerId(eventId, photographerId)).isTrue();
        assertThat(adapter.existsByIdAndPhotographerId(eventId, PhotographerId.generate())).isFalse();

        adapter.deleteById(eventId);
        assertThat(adapter.findById(eventId)).isEmpty();
    }
}
