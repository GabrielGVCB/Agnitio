package com.agnitio.infrastructure.persistence.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataEventRepository extends JpaRepository<EventJpaEntity, UUID> {
    List<EventJpaEntity> findByPhotographerIdOrderByCreatedAtDesc(UUID photographerId);
    boolean existsByIdAndPhotographerId(UUID id, UUID photographerId);
}
