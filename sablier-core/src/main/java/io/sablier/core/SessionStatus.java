package io.sablier.core;

/**
 * Lifecycle status of a {@link Session}.
 *
 * <p>{@code STARTING} and {@code EXPIRING} are intentional placeholders: no Phase 1 code path
 * assigns them yet (session creation resolves synchronously straight to {@code ACTIVE} or
 * throws; there is no expiry-warning scheduler yet) — they exist now because later phases
 * (async start decoupling, an expiry-warning scheduler) need them without a breaking enum change.
 */
public enum SessionStatus {
    STARTING,
    ACTIVE,
    EXPIRING,
    EXPIRED,
    TERMINATED,
    FAILED;

    /** {@code true} for a status a session never leaves once reached. */
    public boolean isTerminal() {
        return this == EXPIRED || this == TERMINATED || this == FAILED;
    }
}
