package com.agnitio.infrastructure.persistence.event;

import com.agnitio.application.dto.PublicEventPage;
import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photographer.PhotographerId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Component
public class EventRepositoryAdapter implements EventRepository {

    private final SpringDataEventRepository repository;

    public EventRepositoryAdapter(SpringDataEventRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Event save(Event event) {
        EventJpaEntity entity = EventJpaEntity.fromDomain(event);
        EventJpaEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Event> findById(EventId id) {
        return repository.findById(id.value()).map(EventJpaEntity::toDomain);
    }

    @Override
    public List<Event> findByPhotographerId(PhotographerId photographerId) {
        return repository.findByPhotographerIdOrderByCreatedAtDesc(photographerId.value())
                .stream()
                .map(EventJpaEntity::toDomain)
                .toList();
    }

    @Override
    public PublicEventPage searchPublic(String query, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Slice<EventJpaEntity> slice = (query == null || query.isBlank())
                ? repository.findPublicListed(pageRequest)
                : repository.searchPublicListed(toContainsPattern(query), pageRequest);

        List<Event> events = slice.getContent().stream().map(EventJpaEntity::toDomain).toList();
        return new PublicEventPage(events, page, size, slice.hasNext());
    }

    /** Builds a case-insensitive "contains" LIKE pattern, escaping the user's wildcard characters. */
    private static String toContainsPattern(String query) {
        String escaped = query.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    @Override
    public boolean existsByIdAndPhotographerId(EventId id, PhotographerId photographerId) {
        return repository.existsByIdAndPhotographerId(id.value(), photographerId.value());
    }

    @Override
    public void deleteById(EventId id) {
        repository.deleteById(id.value());
    }
}
