package io.virtualization.sdk.iam;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record Role(RoleId id, String name, Set<Permission> permissions, Instant createdAt) {

    public Role {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
