package io.virtualization.sdk.core.image;

import java.util.Objects;

/** A human-friendly name that resolves to an {@link ImageId} (e.g. Incus {@code "ubuntu/24.04"}, a Docker tag). */
public record ImageAlias(String name, ImageId target) {

    public ImageAlias {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(target, "target must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
