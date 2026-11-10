package io.virtualization.sdk.storage;

import java.util.Map;
import java.util.Objects;

/** Input to {@link StorageProvider#createVolume} — always creates a {@link VolumeType#CUSTOM} volume. */
public record VolumeSpec(String name, VolumeContentType contentType, String size, Map<String, String> config) {

    public VolumeSpec {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        config = config == null ? Map.of() : Map.copyOf(config);
    }
}
