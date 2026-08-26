package io.virtualization.sdk.core;

import java.util.Objects;

/** A physical or virtual host managed by a provider. */
public record Node(String id, String name) {

    public Node {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
