package io.sablier.core;

import io.sablier.core.exception.OperationException;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * Polls a readiness signal until it reports {@link ReadinessState#READY} or {@link
 * ReadinessPolicy}'s bounds (timeout, max attempts) are exhausted — never loops forever.
 * Deliberately decoupled from {@link ReadinessChecker}/{@link Workload}: the polled signal is any
 * {@code Supplier<ReadinessStatus>}, so the same loop drives both a standalone {@link
 * ReadinessChecker#check} call and a provider's own {@code readiness(id)} method (see {@code
 * SessionManager}, which uses the latter).
 */
public final class ReadinessAwaiter {

    private ReadinessAwaiter() {}

    /**
     * @throws OperationException if the calling thread is interrupted while waiting between polls
     */
    public static ReadinessStatus await(Supplier<ReadinessStatus> poll, ReadinessPolicy policy) {
        Instant deadline = Instant.now().plus(policy.timeout());
        ReadinessStatus last = ReadinessStatus.pending("not yet checked");
        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            last = poll.get();
            if (last.state() == ReadinessState.READY) {
                return last;
            }
            boolean lastAttempt = attempt == policy.maxAttempts() || !Instant.now().isBefore(deadline);
            if (lastAttempt) {
                break;
            }
            sleep(policy.pollInterval());
        }
        return ReadinessStatus.failed("Readiness not reached: " + last.message());
    }

    private static void sleep(java.time.Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperationException("Interrupted while waiting for readiness", e);
        }
    }
}
