package io.virtualization.sdk.iam;

import java.util.Objects;
import java.util.UUID;

public record UserId(String value) {

    public UserId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static UserId generate() {
        return new UserId("user-" + UUID.randomUUID());
    }
}
