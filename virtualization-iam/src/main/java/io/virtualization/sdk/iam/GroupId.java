package io.virtualization.sdk.iam;

import java.util.Objects;
import java.util.UUID;

public record GroupId(String value) {

    public GroupId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static GroupId generate() {
        return new GroupId("group-" + UUID.randomUUID());
    }
}
