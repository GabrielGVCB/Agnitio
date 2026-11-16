package com.agnitio.infrastructure.persistence.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataEventRepository extends JpaRepository<EventJpaEntity, UUID> {
    List<EventJpaEntity> findByPhotographerIdOrderByCreatedAtDesc(UUID photographerId);
    boolean existsByIdAndPhotographerId(UUID id, UUID photographerId);

    @Query("""
            SELECT e FROM EventJpaEntity e
            WHERE e.visibility = com.agnitio.domain.event.EventVisibility.PUBLIC
              AND e.status = com.agnitio.domain.event.EventStatus.ACTIVE
            ORDER BY e.eventDate DESC NULLS LAST, e.createdAt DESC
            """)
    Slice<EventJpaEntity> findPublicListed(Pageable pageable);

    /**
     * @param pattern lower-case LIKE pattern, with '%', '_' and '\' already escaped by the caller
     */
    @Query("""
            SELECT e FROM EventJpaEntity e
            WHERE e.visibility = com.agnitio.domain.event.EventVisibility.PUBLIC
              AND e.status = com.agnitio.domain.event.EventStatus.ACTIVE
              AND (LOWER(e.name) LIKE :pattern ESCAPE '\\'
                   OR LOWER(COALESCE(e.city, '')) LIKE :pattern ESCAPE '\\')
            ORDER BY e.eventDate DESC NULLS LAST, e.createdAt DESC
            """)
    Slice<EventJpaEntity> searchPublicListed(@Param("pattern") String pattern, Pageable pageable);
}
