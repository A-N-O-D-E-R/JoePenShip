package io.virtualization.sdk.loadbalancer;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class HealthCheckTest {

    @Test
    void rejectsBlankPath() {
        assertThatIllegalArgumentException().isThrownBy(() -> new HealthCheck("", Duration.ofSeconds(10), Duration.ofSeconds(5)));
    }

    @Test
    void rejectsNonPositiveInterval() {
        assertThatIllegalArgumentException().isThrownBy(() -> new HealthCheck("/healthz", Duration.ZERO, Duration.ofSeconds(5)));
    }

    @Test
    void rejectsNonPositiveTimeout() {
        assertThatIllegalArgumentException().isThrownBy(() -> new HealthCheck("/healthz", Duration.ofSeconds(10), Duration.ofSeconds(-1)));
    }
}
