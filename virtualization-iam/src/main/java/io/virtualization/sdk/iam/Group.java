package io.virtualization.sdk.iam;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record Group(GroupId id, String name, Set<RoleId> roles, Set<UserId> members, Instant createdAt) {

    public Group {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        members = members == null ? Set.of() : Set.copyOf(members);
    }
}
