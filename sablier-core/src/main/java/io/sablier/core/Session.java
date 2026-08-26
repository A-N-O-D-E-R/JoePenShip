package io.sablier.core;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A user's claim on a workload (or workload group) for a bounded duration. {@link #workloadId()}
 * is {@link Optional} to leave room for a future phase that creates a session before its
 * workload is resolved (e.g. a queued request); {@link SessionManager} in this phase always
 * resolves the workload before ever constructing a {@code Session}, so it is always present in
 * practice today.
 */
public record Session(
        String id, String group, Optional<String> workloadId, Instant createdAt, Instant expiresAt, SessionStatus status) {

    public Session {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(workloadId, "workloadId must not be null (use Optional.empty())");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (group.isBlank()) {
            throw new IllegalArgumentException("group must not be blank");
        }
    }
}
