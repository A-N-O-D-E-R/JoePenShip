package io.virtualization.sdk.core.image;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * A provider-neutral view of an image usable to create a workload.
 *
 * <p>{@code architecture}, {@code os}, {@code distribution}, {@code version} and {@code createdAt}
 * are {@code null} when the provider does not expose that piece of metadata. Provider-specific
 * metadata that has no normalized field goes in {@code metadata} rather than being discarded.
 */
public record Image(
        ImageId id,
        String name,
        ImageType type,
        String architecture,
        String os,
        String distribution,
        String version,
        long size,
        Instant createdAt,
        Map<String, String> metadata) {

    public Image {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative, was " + size);
        }
        metadata = Map.copyOf(metadata);
    }
}
