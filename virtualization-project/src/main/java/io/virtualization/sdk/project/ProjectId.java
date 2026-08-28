package io.virtualization.sdk.project;

import java.util.Objects;
import java.util.UUID;

public record ProjectId(String value) {

    public ProjectId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static ProjectId generate() {
        return new ProjectId("project-" + UUID.randomUUID());
    }
}
