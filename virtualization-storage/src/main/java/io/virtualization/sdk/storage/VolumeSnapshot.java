package io.virtualization.sdk.storage;

import java.time.Instant;
import java.util.Objects;

/** {@code createdAt} is nullable — some backend API versions omit it for custom-volume snapshots. */
public record VolumeSnapshot(String pool, String volumeName, String name, Instant createdAt) {

    public VolumeSnapshot {
        Objects.requireNonNull(pool, "pool must not be null");
        Objects.requireNonNull(volumeName, "volumeName must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }
}
