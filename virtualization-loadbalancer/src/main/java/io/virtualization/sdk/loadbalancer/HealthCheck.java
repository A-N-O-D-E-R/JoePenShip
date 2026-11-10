package io.virtualization.sdk.loadbalancer;

import java.time.Duration;
import java.util.Objects;

/** An active health check against {@code path}. Durations are formatted as whole seconds only. */
public record HealthCheck(String path, Duration interval, Duration timeout) {

    public HealthCheck {
        Objects.requireNonNull(path, "path must not be null");
        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        Objects.requireNonNull(interval, "interval must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("interval must be positive");
        }
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }
}
