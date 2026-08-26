package io.virtualization.sdk.core;

import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.VirtualizationException;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * A read-only view of an asynchronous lifecycle operation (start, stop, reboot, ...) in progress
 * on a provider.
 *
 * <p>Backends complete operations from whatever mechanism they use internally (task polling,
 * WebSocket events, QMP events) via the corresponding {@link OperationHandle} — consumers only
 * ever see this read-only view.
 *
 * <p>{@link #await()} never throws for a {@link OperationStatus#FAILED} outcome; callers inspect
 * {@link #status()} and {@link #error()} themselves. {@link #await(Duration)} throws
 * {@link OperationException} only if the wait times out, since a timeout is a genuinely different,
 * exceptional outcome from a known terminal failure.
 */
public interface Operation {

    String id();

    OperationStatus status();

    OptionalDouble progress();

    Optional<VirtualizationException> error();

    /**
     * Blocks until this operation reaches a terminal state.
     *
     * @return the terminal {@link OperationStatus} ({@link OperationStatus#SUCCEEDED} or
     *     {@link OperationStatus#FAILED})
     * @throws OperationException if the calling thread is interrupted while waiting
     */
    OperationStatus await();

    /**
     * Blocks until this operation reaches a terminal state or the timeout elapses.
     *
     * @throws OperationException if the wait times out, or the calling thread is interrupted
     */
    OperationStatus await(Duration timeout);
}
