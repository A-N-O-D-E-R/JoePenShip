package io.virtualization.sdk.core;

import java.util.Objects;

/** A group of {@link Node}s managed as a single unit by a provider. */
public record Cluster(String id, String name) {

    public Cluster {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
