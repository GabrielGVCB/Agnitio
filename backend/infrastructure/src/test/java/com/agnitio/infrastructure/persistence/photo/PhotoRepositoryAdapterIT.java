package com.agnitio.infrastructure.persistence.photo;

import com.agnitio.domain.event.EventId;
import com.agnitio.domain.photo.Photo;
import com.agnitio.domain.photo.PhotoId;
import com.agnitio.domain.photo.PhotoStatus;
import org.junit.jupiter.api.DisplayName;
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

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PhotoRepositoryAdapter.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PhotoRepositoryAdapterIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private PhotoRepositoryAdapter photoRepositoryAdapter;

    @Test
    @DisplayName("Should successfully persist domain Photo and retrieve it via adapter")
    void shouldPersistAndRetrievePhoto() {
        PhotoId photoId = PhotoId.generate();
        EventId eventId = EventId.generate();
        Photo photo = new Photo(photoId, eventId, "runner.jpg", Instant.now(), PhotoStatus.UPLOADED);

        photoRepositoryAdapter.save(photo);

        Optional<Photo> retrieved = photoRepositoryAdapter.findById(photoId);

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo(photoId);
        assertThat(retrieved.get().getEventId()).isEqualTo(eventId);
        assertThat(retrieved.get().getOriginalFileName()).isEqualTo("runner.jpg");
        assertThat(retrieved.get().getStatus()).isEqualTo(PhotoStatus.UPLOADED);
    }
}
