package com.agnitio.infrastructure.persistence.photo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataPhotoRepository extends JpaRepository<PhotoJpaEntity, UUID> {
}
