package io.virtualization.sdk.core.image.internal;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.exception.VirtualizationException;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Shared boilerplate for the image/workload operation types that compose a plain {@link
 * OperationHandle} for status/progress/error/await, rather than reimplementing it.
 */
abstract class ComposedOperation implements Operation {

    private final OperationHandle handle;
    private final Operation operation;

    protected ComposedOperation(String id) {
        this.handle = OperationHandle.create(id);
        this.operation = handle.operation();
    }

    @Override
    public String id() {
        return operation.id();
    }

    @Override
    public OperationStatus status() {
        return operation.status();
    }

    @Override
    public OptionalDouble progress() {
        return operation.progress();
    }

    @Override
    public Optional<VirtualizationException> error() {
        return operation.error();
    }

    @Override
    public OperationStatus await() {
        return operation.await();
    }

    @Override
    public OperationStatus await(Duration timeout) {
        return operation.await(timeout);
    }

    protected final void updateProgressInternal(double progress) {
        handle.updateProgress(progress);
    }

    protected final void completeInternal() {
        handle.complete();
    }

    protected final void failInternal(VirtualizationException cause) {
        handle.fail(cause);
    }
}
