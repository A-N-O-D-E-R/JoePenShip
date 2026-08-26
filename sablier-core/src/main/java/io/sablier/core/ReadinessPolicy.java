package io.sablier.core;

import java.time.Duration;
import java.util.Objects;

/**
 * Bounds a readiness wait: an overall {@link #timeout()}, a {@link #pollInterval()} between
 * checks, and a hard {@link #maxAttempts()} cap — three independent bounds so a readiness wait
 * can never loop forever even if clock/interval math were somehow off (spec section 19: "avoid
 * infinite loops").
 */
public record ReadinessPolicy(Duration timeout, Duration pollInterval, int maxAttempts) {

    public ReadinessPolicy {
        Objects.requireNonNull(timeout, "timeout must not be null");
        Objects.requireNonNull(pollInterval, "pollInterval must not be null");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (pollInterval.isNegative() || pollInterval.isZero()) {
            throw new IllegalArgumentException("pollInterval must be positive");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
    }

    /** 60s timeout, 2s poll interval, 30 max attempts — matches the spec's own config example defaults. */
    public static ReadinessPolicy defaults() {
        return new ReadinessPolicy(Duration.ofSeconds(60), Duration.ofSeconds(2), 30);
    }
}
