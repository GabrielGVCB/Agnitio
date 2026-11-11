package com.agnitio.infrastructure.persistence.photographer;

import com.agnitio.application.ports.output.PhotographerRepository;
import com.agnitio.domain.photographer.Photographer;
import com.agnitio.domain.photographer.PhotographerId;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class PhotographerRepositoryAdapter implements PhotographerRepository {

    private final SpringDataPhotographerRepository repository;

    public PhotographerRepositoryAdapter(SpringDataPhotographerRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Photographer save(Photographer photographer) {
        PhotographerJpaEntity entity = PhotographerJpaEntity.fromDomain(photographer);
        PhotographerJpaEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Photographer> findById(PhotographerId id) {
        return repository.findById(id.value()).map(PhotographerJpaEntity::toDomain);
    }

    @Override
    public Optional<Photographer> findByEmail(String email) {
        return repository.findByEmail(email).map(PhotographerJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }
}
