package com.agnitio.domain.photo;

import java.util.Objects;
import java.util.UUID;

public record PhotoId(UUID value) {
    public PhotoId {
        Objects.requireNonNull(value, "PhotoId value must not be null");
    }

    public static PhotoId generate() {
        return new PhotoId(UUID.randomUUID());
    }

    public static PhotoId of(String value) {
        return new PhotoId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
