package io.sablier.core;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Provider-neutral view of the activation metadata a backend attaches to a workload (e.g. Incus
 * instance configuration keys such as {@code user.sablier.enable}). Provider adapters map their
 * own configuration convention onto this type; {@code sablier-core} has no opinion on what that
 * convention looks like.
 */
public record WorkloadMetadata(boolean enabled, String group, Optional<Duration> defaultDuration, Optional<String> readinessCheck) {

    public WorkloadMetadata {
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(defaultDuration, "defaultDuration must not be null (use Optional.empty())");
        Objects.requireNonNull(readinessCheck, "readinessCheck must not be null (use Optional.empty())");
        if (group.isBlank()) {
            throw new IllegalArgumentException("group must not be blank");
        }
    }
}
