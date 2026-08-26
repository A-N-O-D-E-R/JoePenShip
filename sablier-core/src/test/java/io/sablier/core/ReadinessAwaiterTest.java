package io.sablier.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ReadinessAwaiterTest {

    @Test
    void returnsImmediatelyWhenAlreadyReady() {
        ReadinessPolicy policy = new ReadinessPolicy(Duration.ofSeconds(1), Duration.ofMillis(10), 5);

        ReadinessStatus status = ReadinessAwaiter.await(ReadinessStatus::ready, policy);

        assertThat(status.state()).isEqualTo(ReadinessState.READY);
    }

    @Test
    void succeedsAfterAFewRetries() {
        ReadinessPolicy policy = new ReadinessPolicy(Duration.ofSeconds(2), Duration.ofMillis(10), 10);
        AtomicInteger attempts = new AtomicInteger();

        ReadinessStatus status = ReadinessAwaiter.await(
                () -> attempts.incrementAndGet() < 3 ? ReadinessStatus.pending("not yet") : ReadinessStatus.ready(), policy);

        assertThat(status.state()).isEqualTo(ReadinessState.READY);
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void returnsFailedAfterExhaustingMaxAttempts() {
        ReadinessPolicy policy = new ReadinessPolicy(Duration.ofSeconds(5), Duration.ofMillis(5), 3);
        AtomicInteger attempts = new AtomicInteger();

        ReadinessStatus status = ReadinessAwaiter.await(
                () -> {
                    attempts.incrementAndGet();
                    return ReadinessStatus.pending("never ready");
                },
                policy);

        assertThat(status.state()).isEqualTo(ReadinessState.FAILED);
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void neverExceedsMaxAttemptsEvenWithGenerousTimeout() {
        ReadinessPolicy policy = new ReadinessPolicy(Duration.ofHours(1), Duration.ofMillis(1), 2);
        AtomicInteger attempts = new AtomicInteger();

        ReadinessAwaiter.await(
                () -> {
                    attempts.incrementAndGet();
                    return ReadinessStatus.pending("never ready");
                },
                policy);

        assertThat(attempts.get()).isEqualTo(2);
    }
}
