package io.sablier.core;

import java.time.Duration;
import java.util.Objects;

/**
 * Bounds a {@link SessionManager} enforces on every session: {@link #defaultDuration()} is used
 * when a request omits a duration (reserved for the future HTTP layer — no Phase 1 code path
 * falls back to it yet, {@link SessionRequest#duration()} is currently mandatory), and every
 * session's {@code expiresAt} is clamped so it can never exceed {@code createdAt +
 * maxDuration()}.
 */
public record SessionPolicy(Duration defaultDuration, Duration maxDuration) {

    public SessionPolicy {
        Objects.requireNonNull(defaultDuration, "defaultDuration must not be null");
        Objects.requireNonNull(maxDuration, "maxDuration must not be null");
        if (defaultDuration.isNegative() || defaultDuration.isZero()) {
            throw new IllegalArgumentException("defaultDuration must be positive");
        }
        if (maxDuration.isNegative() || maxDuration.isZero()) {
            throw new IllegalArgumentException("maxDuration must be positive");
        }
        if (maxDuration.compareTo(defaultDuration) < 0) {
            throw new IllegalArgumentException("maxDuration must be >= defaultDuration");
        }
    }
}
