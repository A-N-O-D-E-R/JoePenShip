package io.sablier.core;

import java.time.Duration;
import java.util.Objects;

/** A request to activate a workload group for a given duration, e.g. {@code {"group":"media","duration":"30m"}}. */
public record SessionRequest(String group, Duration duration) {

    public SessionRequest {
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
        if (group.isBlank()) {
            throw new IllegalArgumentException("group must not be blank");
        }
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("duration must be positive");
        }
    }
}
