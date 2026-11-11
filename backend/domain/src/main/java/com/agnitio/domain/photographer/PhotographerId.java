package com.agnitio.domain.photographer;

import java.util.Objects;
import java.util.UUID;

public record PhotographerId(UUID value) {
    public PhotographerId {
        Objects.requireNonNull(value, "PhotographerId value must not be null");
    }

    public static PhotographerId generate() {
        return new PhotographerId(UUID.randomUUID());
    }

    public static PhotographerId of(String value) {
        return new PhotographerId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
