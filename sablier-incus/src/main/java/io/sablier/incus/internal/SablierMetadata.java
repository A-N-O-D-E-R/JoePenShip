package io.sablier.incus.internal;

import io.sablier.core.WorkloadMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * The {@code user.sablier.*} Incus instance configuration convention (spec section 10/11), e.g.:
 * <pre>{@code
 * incus config set jellyfin user.sablier.enable=true
 * incus config set jellyfin user.sablier.group=media
 * }</pre>
 * {@code sablier-core} has no opinion on this convention — it lives entirely in this Incus-adapter
 * package, mapping raw instance config onto the provider-neutral {@link WorkloadMetadata}.
 */
public final class SablierMetadata {

    private static final Logger log = LoggerFactory.getLogger(SablierMetadata.class);

    public static final String ENABLE_KEY = "user.sablier.enable";
    public static final String GROUP_KEY = "user.sablier.group";
    public static final String DURATION_KEY = "user.sablier.duration";
    public static final String READINESS_CHECK_KEY = "user.sablier.readiness-check";

    private SablierMetadata() {}

    /**
     * @return the parsed metadata, or empty if the instance isn't a usable Sablier workload:
     *     not enabled at all (the common, expected case for most Incus instances — not logged),
     *     or enabled but missing {@link #GROUP_KEY} (a likely misconfiguration — logged at WARN).
     */
    public static Optional<WorkloadMetadata> parse(String instanceName, Map<String, String> config) {
        boolean enabled = Boolean.parseBoolean(config.get(ENABLE_KEY));
        if (!enabled) {
            return Optional.empty();
        }
        String group = config.get(GROUP_KEY);
        if (group == null || group.isBlank()) {
            log.warn("Incus instance '{}' has {}=true but no {} set; skipping", instanceName, ENABLE_KEY, GROUP_KEY);
            return Optional.empty();
        }
        Optional<Duration> defaultDuration = Optional.ofNullable(config.get(DURATION_KEY)).flatMap(SablierMetadata::parseDuration);
        Optional<String> readinessCheck = Optional.ofNullable(config.get(READINESS_CHECK_KEY)).filter(s -> !s.isBlank());
        return Optional.of(new WorkloadMetadata(true, group, defaultDuration, readinessCheck));
    }

    private static Optional<Duration> parseDuration(String raw) {
        try {
            return Optional.of(Duration.parse(raw));
        } catch (java.time.format.DateTimeParseException e) {
            log.warn("Ignoring invalid {} value '{}': expected an ISO-8601 duration such as 'PT30M'", DURATION_KEY, raw);
            return Optional.empty();
        }
    }
}
