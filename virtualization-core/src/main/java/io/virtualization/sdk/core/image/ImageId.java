package io.virtualization.sdk.core.image;

import java.util.Objects;

/** The identity a provider assigns to an already-resolved {@link Image} (e.g. a digest or fingerprint). */
public record ImageId(String value) {

    public ImageId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
