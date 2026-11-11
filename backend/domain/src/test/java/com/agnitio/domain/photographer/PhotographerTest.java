package com.agnitio.domain.photographer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhotographerTest {

    @Test
    void shouldCreateActivePhotographerSuccessfully() {
        PhotographerId id = PhotographerId.generate();
        Photographer photographer = Photographer.create(
                id,
                "john@example.com",
                "$argon2id$v=19$m=65536,t=3,p=1$fakehash",
                "John Doe",
                PhotographerRole.PHOTOGRAPHER
        );

        assertThat(photographer.getId()).isEqualTo(id);
        assertThat(photographer.getEmail()).isEqualTo("john@example.com");
        assertThat(photographer.getName()).isEqualTo("John Doe");
        assertThat(photographer.getRole()).isEqualTo(PhotographerRole.PHOTOGRAPHER);
        assertThat(photographer.getStatus()).isEqualTo(PhotographerStatus.ACTIVE);
        assertThat(photographer.isActive()).isTrue();
        assertThat(photographer.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFailWhenEmailIsInvalid() {
        PhotographerId id = PhotographerId.generate();

        assertThatThrownBy(() -> Photographer.create(id, "not-an-email", "hash", "Name", PhotographerRole.PHOTOGRAPHER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        PhotographerId id = PhotographerId.generate();

        assertThatThrownBy(() -> Photographer.create(id, "john@example.com", "hash", "  ", PhotographerRole.PHOTOGRAPHER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name must not be blank");
    }

    @Test
    void shouldDeactivateAndActivatePhotographer() {
        Photographer photographer = Photographer.create(
                PhotographerId.generate(),
                "jane@example.com",
                "hash",
                "Jane Doe",
                PhotographerRole.PHOTOGRAPHER
        );

        photographer.deactivate();
        assertThat(photographer.isActive()).isFalse();
        assertThat(photographer.getStatus()).isEqualTo(PhotographerStatus.INACTIVE);

        assertThatThrownBy(photographer::deactivate)
                .isInstanceOf(InvalidPhotographerStateException.class);

        photographer.activate();
        assertThat(photographer.isActive()).isTrue();
    }
}
