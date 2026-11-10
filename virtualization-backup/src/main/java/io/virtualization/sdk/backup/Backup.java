package io.virtualization.sdk.backup;

import java.time.Instant;
import java.util.Objects;

/** An exportable backup of an instance. {@code expiresAt} is nullable — no expiry. */
public record Backup(String instanceId, String name, Instant createdAt, Instant expiresAt) {

    public Backup {
        Objects.requireNonNull(instanceId, "instanceId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
