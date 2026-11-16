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

import com.agnitio.application.dto.PublicEventPage;

import java.time.LocalDate;
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
                90,
                null,
                null
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
    @Test
    void shouldSearchOnlyActivePublicEventsByNameOrCity() {
        PhotographerId owner = PhotographerId.generate();
        Event rioRun = adapter.save(publicEvent(owner, "Maratona do Rio", LocalDate.of(2026, 5, 12), "Rio de Janeiro, RJ"));
        Event spRun = adapter.save(publicEvent(owner, "Corrida Noturna", LocalDate.of(2026, 6, 1), "São Paulo, SP"));
        Event undated = adapter.save(publicEvent(owner, "Maratona Sem Data", null, null));
        adapter.save(Event.create(EventId.generate(), owner, "Maratona Privada", EventType.RACE,
                EventVisibility.PRIVATE, "SECRET", 30, LocalDate.of(2026, 7, 1), "Rio de Janeiro, RJ"));
        Event archived = publicEvent(owner, "Maratona Arquivada", LocalDate.of(2026, 8, 1), "Rio de Janeiro, RJ");
        archived.archive();
        adapter.save(archived);

        // name match, case-insensitive; private and archived excluded; undated last
        assertThat(ids(adapter.searchPublic("MARATONA", 0, 10)))
                .containsExactly(rioRun.getId(), undated.getId());

        // city match
        assertThat(ids(adapter.searchPublic("são paulo", 0, 10)))
                .containsExactly(spRun.getId());

        // no query lists every listed event, most recent date first
        assertThat(ids(adapter.searchPublic(null, 0, 10)))
                .containsExactly(spRun.getId(), rioRun.getId(), undated.getId());

        // LIKE wildcards typed by the user are treated literally
        assertThat(adapter.searchPublic("%", 0, 10).events()).isEmpty();
    }

    @Test
    void shouldPaginatePublicSearch() {
        PhotographerId owner = PhotographerId.generate();
        for (int day = 1; day <= 3; day++) {
            adapter.save(publicEvent(owner, "Run " + day, LocalDate.of(2026, 1, day), null));
        }

        PublicEventPage first = adapter.searchPublic(null, 0, 2);
        PublicEventPage second = adapter.searchPublic(null, 1, 2);

        assertThat(first.events()).extracting(Event::getName).containsExactly("Run 3", "Run 2");
        assertThat(first.hasNext()).isTrue();
        assertThat(second.events()).extracting(Event::getName).containsExactly("Run 1");
        assertThat(second.hasNext()).isFalse();
    }

    @Test
    void shouldPersistEventDateAndCity() {
        Event saved = adapter.save(publicEvent(PhotographerId.generate(), "Run", LocalDate.of(2026, 3, 9), "Curitiba, PR"));

        Event found = adapter.findById(saved.getId()).orElseThrow();
        assertThat(found.getEventDate()).isEqualTo(LocalDate.of(2026, 3, 9));
        assertThat(found.getCity()).isEqualTo("Curitiba, PR");
    }

    private static Event publicEvent(PhotographerId owner, String name, LocalDate date, String city) {
        return Event.create(EventId.generate(), owner, name, EventType.RACE, EventVisibility.PUBLIC, null, 30, date, city);
    }

    private static List<EventId> ids(PublicEventPage page) {
        return page.events().stream().map(Event::getId).toList();
    }
}
