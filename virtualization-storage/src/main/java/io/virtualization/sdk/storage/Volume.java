package io.virtualization.sdk.storage;

import java.util.Map;
import java.util.Objects;

/**
 * A storage volume within a {@link StoragePool}. {@code size} is a raw backend size string (e.g.
 * {@code "10GB"}), not a structured type — {@code null} means pool default/no quota.
 */
public record Volume(String pool, String name, VolumeType type, VolumeContentType contentType, String size, Map<String, String> config) {

    public Volume {
        Objects.requireNonNull(pool, "pool must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
        config = config == null ? Map.of() : Map.copyOf(config);
    }
}
