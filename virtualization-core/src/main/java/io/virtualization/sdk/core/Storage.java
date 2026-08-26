package io.virtualization.sdk.core;

import java.util.Objects;

/** A storage pool or volume exposed by a provider. */
public record Storage(String id, String name) {

    public Storage {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
