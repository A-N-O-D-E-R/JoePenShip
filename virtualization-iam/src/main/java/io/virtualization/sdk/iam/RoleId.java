package io.virtualization.sdk.iam;

import java.util.Objects;
import java.util.UUID;

public record RoleId(String value) {

    public RoleId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static RoleId generate() {
        return new RoleId("role-" + UUID.randomUUID());
    }
}
