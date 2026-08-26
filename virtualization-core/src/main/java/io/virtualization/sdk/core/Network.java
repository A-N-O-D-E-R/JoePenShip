package io.virtualization.sdk.core;

import java.util.Objects;

/** A network exposed by a provider. */
public record Network(String id, String name) {

    public Network {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
