package io.virtualization.sdk.core;

import java.time.Instant;
import java.util.Objects;

/** A point-in-time snapshot of a {@link VirtualMachine} or {@link Container}. */
public record Snapshot(String id, String name, Instant createdAt) {

    public Snapshot {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
