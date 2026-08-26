package io.virtualization.sdk.core;

import java.util.Objects;

/** A template or disk image usable to create a new {@link VirtualMachine} or {@link Container}. */
public record Image(String id, String name) {

    public Image {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
