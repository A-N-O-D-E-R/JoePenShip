package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.image.ImageReference;

import java.time.Instant;
import java.util.Objects;

/**
 * A provider-neutral VPS: image + compute + storage + network + lifecycle, orchestrated above
 * {@code VirtualizationProvider}/{@code ImageProvider}.
 *
 * <p>{@code provider}/{@code project}/{@code workloadId} are {@code null} until provisioning
 * completes. {@code spec} is the originating {@link VpsSpec} — kept for rebuild and audit.
 * {@code startedAt}/{@code stoppedAt}/{@code destroyedAt} are {@code null} until that transition
 * has happened at least once.
 */
public record Vps(
        VpsId id,
        String name,
        VpsState state,
        VpsType type,
        ImageReference image,
        ComputeResources compute,
        StorageConfiguration storage,
        NetworkConfiguration network,
        VpsSpec spec,
        String provider,
        String project,
        String workloadId,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant stoppedAt,
        Instant destroyedAt) {

    public Vps {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(image, "image must not be null");
        Objects.requireNonNull(compute, "compute must not be null");
        Objects.requireNonNull(storage, "storage must not be null");
        Objects.requireNonNull(network, "network must not be null");
        Objects.requireNonNull(spec, "spec must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
