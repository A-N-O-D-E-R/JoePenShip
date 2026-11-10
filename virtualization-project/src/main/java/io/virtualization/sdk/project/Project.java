package io.virtualization.sdk.project;

import java.time.Instant;
import java.util.Objects;

/**
 * A provider-neutral grouping of resources — {@code name} is unique (enforced by {@link
 * DefaultProjectManager#create}), {@code description} is optional free text.
 */
public record Project(ProjectId id, String name, String description, Instant createdAt) {

    public Project {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
