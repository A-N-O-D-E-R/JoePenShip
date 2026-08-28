package io.virtualization.sdk.backup;

import java.time.Instant;
import java.util.Objects;

/**
 * A point-in-time snapshot of an instance. {@code instanceId} is a raw provider-native
 * identifier (this SDK's {@code Vps.workloadId()}, not a typed {@code VpsId}) — keeps this
 * module independent of {@code virtualization-vps}.
 */
public record Snapshot(String instanceId, String name, boolean stateful, Instant createdAt) {

    public Snapshot {
        Objects.requireNonNull(instanceId, "instanceId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
