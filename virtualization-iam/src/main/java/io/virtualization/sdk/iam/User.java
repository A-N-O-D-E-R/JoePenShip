package io.virtualization.sdk.iam;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * A local IAM account. {@code passwordHash} is a {@link PasswordHasher}-encoded string, never a
 * plaintext password — {@link #toString()} redacts it, same contract as {@code
 * CertificateMaterial}'s redacted {@code toString()} in {@code virtualization-certificate}.
 */
public record User(UserId id, String username, String passwordHash, Set<RoleId> roles, Set<GroupId> groups, boolean enabled, Instant createdAt) {

    public User {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        groups = groups == null ? Set.of() : Set.copyOf(groups);
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username + ", passwordHash=<redacted>, roles=" + roles + ", groups=" + groups
                + ", enabled=" + enabled + ", createdAt=" + createdAt + "]";
    }
}
