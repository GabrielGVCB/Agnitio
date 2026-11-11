package com.agnitio.infrastructure.persistence.photographer;

import com.agnitio.domain.photographer.Photographer;
import com.agnitio.domain.photographer.PhotographerId;
import com.agnitio.domain.photographer.PhotographerRole;
import com.agnitio.domain.photographer.PhotographerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "photographers")
public class PhotographerJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private PhotographerRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PhotographerStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PhotographerJpaEntity() {
    }

    public PhotographerJpaEntity(
            UUID id,
            String email,
            String passwordHash,
            String name,
            PhotographerRole role,
            PhotographerStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PhotographerJpaEntity fromDomain(Photographer photographer) {
        return new PhotographerJpaEntity(
                photographer.getId().value(),
                photographer.getEmail(),
                photographer.getPasswordHash(),
                photographer.getName(),
                photographer.getRole(),
                photographer.getStatus(),
                photographer.getCreatedAt(),
                photographer.getUpdatedAt()
        );
    }

    public Photographer toDomain() {
        return new Photographer(
                new PhotographerId(this.id),
                this.email,
                this.passwordHash,
                this.name,
                this.role,
                this.status,
                this.createdAt,
                this.updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PhotographerRole getRole() {
        return role;
    }

    public void setRole(PhotographerRole role) {
        this.role = role;
    }

    public PhotographerStatus getStatus() {
        return status;
    }

    public void setStatus(PhotographerStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
