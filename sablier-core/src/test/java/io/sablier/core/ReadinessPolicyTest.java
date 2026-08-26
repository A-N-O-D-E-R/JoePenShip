package io.sablier.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ReadinessPolicyTest {

    @Test
    void constructsWithValidValues() {
        ReadinessPolicy policy = new ReadinessPolicy(Duration.ofSeconds(60), Duration.ofSeconds(2), 30);

        assertThat(policy.timeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(policy.maxAttempts()).isEqualTo(30);
    }

    @Test
    void defaultsFactoryProducesValidPolicy() {
        assertThat(ReadinessPolicy.defaults().maxAttempts()).isPositive();
    }

    @Test
    void rejectsNonPositiveTimeoutOrInterval() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ReadinessPolicy(Duration.ZERO, Duration.ofSeconds(2), 30));
        assertThatIllegalArgumentException().isThrownBy(() -> new ReadinessPolicy(Duration.ofSeconds(60), Duration.ZERO, 30));
    }

    @Test
    void rejectsNonPositiveMaxAttempts() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ReadinessPolicy(Duration.ofSeconds(60), Duration.ofSeconds(2), 0));
    }
}
