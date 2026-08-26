package io.sablier.incus.internal;

import io.sablier.core.WorkloadMetadata;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SablierMetadataTest {

    @Test
    void notEnabledReturnsEmpty() {
        assertThat(SablierMetadata.parse("jellyfin", Map.of())).isEmpty();
        assertThat(SablierMetadata.parse("jellyfin", Map.of("user.sablier.enable", "false"))).isEmpty();
    }

    @Test
    void enabledWithoutGroupReturnsEmpty() {
        assertThat(SablierMetadata.parse("jellyfin", Map.of("user.sablier.enable", "true"))).isEmpty();
    }

    @Test
    void enabledWithGroupParsesCorrectly() {
        Optional<WorkloadMetadata> metadata =
                SablierMetadata.parse("jellyfin", Map.of("user.sablier.enable", "true", "user.sablier.group", "media"));

        assertThat(metadata).isPresent();
        assertThat(metadata.get().enabled()).isTrue();
        assertThat(metadata.get().group()).isEqualTo("media");
        assertThat(metadata.get().defaultDuration()).isEmpty();
        assertThat(metadata.get().readinessCheck()).isEmpty();
    }

    @Test
    void parsesValidIso8601Duration() {
        Optional<WorkloadMetadata> metadata = SablierMetadata.parse(
                "jellyfin",
                Map.of("user.sablier.enable", "true", "user.sablier.group", "media", "user.sablier.duration", "PT30M"));

        assertThat(metadata.get().defaultDuration()).hasValue(Duration.ofMinutes(30));
    }

    @Test
    void invalidDurationIsIgnoredNotFatal() {
        Optional<WorkloadMetadata> metadata = SablierMetadata.parse(
                "jellyfin",
                Map.of("user.sablier.enable", "true", "user.sablier.group", "media", "user.sablier.duration", "30m"));

        assertThat(metadata).isPresent();
        assertThat(metadata.get().defaultDuration()).isEmpty();
    }

    @Test
    void parsesReadinessCheckKey() {
        Optional<WorkloadMetadata> metadata = SablierMetadata.parse(
                "jellyfin",
                Map.of(
                        "user.sablier.enable", "true",
                        "user.sablier.group", "media",
                        "user.sablier.readiness-check", "http://10.0.0.20:8096/health"));

        assertThat(metadata.get().readinessCheck()).hasValue("http://10.0.0.20:8096/health");
    }
}
