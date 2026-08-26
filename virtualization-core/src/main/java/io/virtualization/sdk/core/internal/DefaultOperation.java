package io.virtualization.sdk.core.internal;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.VirtualizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Internal implementation shared by {@link Operation} and {@link OperationHandle}. Not part of
 * the public API — obtain instances via {@link OperationHandle#create(String)}.
 *
 * <p>{@link #complete()} and {@link #fail} both resolve the backing future normally (never
 * exceptionally), which is what makes {@link Operation#await()} never throw on a
 * {@link OperationStatus#FAILED} outcome.
 */
public final class DefaultOperation implements Operation, OperationHandle {

    private static final Logger log = LoggerFactory.getLogger(DefaultOperation.class);

    private final String id;
    private final CompletableFuture<Void> future = new CompletableFuture<>();
    private volatile OperationStatus status = OperationStatus.RUNNING;
    private volatile VirtualizationException error;
    private volatile double progress = -1;

    public DefaultOperation(String id) {
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    @Override
    public Operation operation() {
        return this;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public OperationStatus status() {
        return status;
    }

    @Override
    public OptionalDouble progress() {
        double current = progress;
        return current < 0 ? OptionalDouble.empty() : OptionalDouble.of(current);
    }

    @Override
    public Optional<VirtualizationException> error() {
        return Optional.ofNullable(error);
    }

    @Override
    public void updateProgress(double progress) {
        if (progress < 0.0 || progress > 1.0) {
            throw new IllegalArgumentException("progress must be within [0.0, 1.0], was " + progress);
        }
        this.progress = progress;
    }

    @Override
    public void complete() {
        status = OperationStatus.SUCCEEDED;
        future.complete(null);
    }

    @Override
    public void fail(VirtualizationException cause) {
        error = Objects.requireNonNull(cause, "cause must not be null");
        status = OperationStatus.FAILED;
        log.debug("Operation {} failed", id, cause);
        future.complete(null);
    }

    @Override
    public OperationStatus await() {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperationException("Interrupted while awaiting operation " + id, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unreachable: operation future never completes exceptionally", e);
        }
        return status;
    }

    @Override
    public OperationStatus await(Duration timeout) {
        try {
            future.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperationException("Interrupted while awaiting operation " + id, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unreachable: operation future never completes exceptionally", e);
        } catch (TimeoutException e) {
            throw new OperationException("Timed out awaiting operation " + id, e);
        }
        return status;
    }
}
