package io.sablier.incus.internal;

import io.sablier.core.Operation;
import io.sablier.core.OperationStatus;
import io.sablier.core.exception.OperationException;
import io.sablier.core.exception.SablierException;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;

/** Adapts a {@code virtualization-sdk} {@link io.virtualization.sdk.core.Operation} to {@code sablier-core}'s {@link Operation}. */
public final class OperationAdapter implements Operation {

    private final io.virtualization.sdk.core.Operation delegate;

    public OperationAdapter(io.virtualization.sdk.core.Operation delegate) {
        this.delegate = delegate;
    }

    @Override
    public String id() {
        return delegate.id();
    }

    @Override
    public OperationStatus status() {
        return map(delegate.status());
    }

    @Override
    public OptionalDouble progress() {
        return delegate.progress();
    }

    @Override
    public Optional<SablierException> error() {
        return delegate.error().map(e -> new OperationException(e.getMessage(), e));
    }

    @Override
    public OperationStatus await() {
        return map(delegate.await());
    }

    @Override
    public OperationStatus await(Duration timeout) {
        try {
            return map(delegate.await(timeout));
        } catch (io.virtualization.sdk.core.exception.OperationException e) {
            throw new OperationException(e.getMessage(), e);
        }
    }

    private static OperationStatus map(io.virtualization.sdk.core.OperationStatus status) {
        return switch (status) {
            case RUNNING -> OperationStatus.RUNNING;
            case SUCCEEDED -> OperationStatus.SUCCEEDED;
            case FAILED -> OperationStatus.FAILED;
        };
    }
}
