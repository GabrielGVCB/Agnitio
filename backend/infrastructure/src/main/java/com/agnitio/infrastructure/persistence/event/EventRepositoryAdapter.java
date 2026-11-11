package com.agnitio.infrastructure.persistence.event;

import com.agnitio.application.ports.output.EventRepository;
import com.agnitio.domain.event.Event;
import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photographer.PhotographerId;
import org.springframework.stereotype.Component;

import java.util.List;
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
    public boolean existsByIdAndPhotographerId(EventId id, PhotographerId photographerId) {
        return repository.existsByIdAndPhotographerId(id.value(), photographerId.value());
    }

    @Override
    public void deleteById(EventId id) {
        repository.deleteById(id.value());
    }
}
