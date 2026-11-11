package com.agnitio.infrastructure.persistence.photographer;

import com.agnitio.domain.photographer.Photographer;
import com.agnitio.domain.photographer.PhotographerId;
import com.agnitio.domain.photographer.PhotographerRole;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PhotographerRepositoryAdapter.class)
@Testcontainers
class PhotographerRepositoryAdapterIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("test_photographers")
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
    private PhotographerRepositoryAdapter adapter;

    @Test
    void shouldPersistAndFindPhotographer() {
        PhotographerId id = PhotographerId.generate();
        Photographer photographer = Photographer.create(
                id,
                "gabriel@agnitio.com",
                "$argon2id$v=19$m=65536,t=3,p=1$fakehash",
                "Gabriel",
                PhotographerRole.PHOTOGRAPHER
        );

        adapter.save(photographer);

        Optional<Photographer> foundById = adapter.findById(id);
        assertThat(foundById).isPresent();
        assertThat(foundById.get().getEmail()).isEqualTo("gabriel@agnitio.com");
        assertThat(foundById.get().getName()).isEqualTo("Gabriel");

        Optional<Photographer> foundByEmail = adapter.findByEmail("gabriel@agnitio.com");
        assertThat(foundByEmail).isPresent();
        assertThat(foundByEmail.get().getId()).isEqualTo(id);

        assertThat(adapter.existsByEmail("gabriel@agnitio.com")).isTrue();
        assertThat(adapter.existsByEmail("other@agnitio.com")).isFalse();
    }
}
