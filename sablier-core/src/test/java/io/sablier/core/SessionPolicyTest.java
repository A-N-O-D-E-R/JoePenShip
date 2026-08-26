package io.sablier.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SessionPolicyTest {

    @Test
    void constructsWithValidDurations() {
        SessionPolicy policy = new SessionPolicy(Duration.ofMinutes(30), Duration.ofHours(8));
        assertThat(policy.defaultDuration()).isEqualTo(Duration.ofMinutes(30));
        assertThat(policy.maxDuration()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void acceptsMaxEqualToDefault() {
        SessionPolicy policy = new SessionPolicy(Duration.ofMinutes(30), Duration.ofMinutes(30));
        assertThat(policy.maxDuration()).isEqualTo(policy.defaultDuration());
    }

    @Test
    void rejectsNonPositiveDurations() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SessionPolicy(Duration.ZERO, Duration.ofHours(8)));
        assertThatIllegalArgumentException().isThrownBy(() -> new SessionPolicy(Duration.ofMinutes(30), Duration.ZERO));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SessionPolicy(Duration.ofMinutes(-1), Duration.ofHours(8)));
    }

    @Test
    void rejectsMaxLessThanDefault() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SessionPolicy(Duration.ofHours(8), Duration.ofMinutes(30)));
    }
}
