package io.virtualization.sdk.core.image.internal;

import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.core.image.CreateWorkloadHandle;
import io.virtualization.sdk.core.image.CreateWorkloadOperation;

import java.util.Optional;

/**
 * Not part of the public API — obtain instances via {@link CreateWorkloadHandle#create(String)}.
 */
public final class DefaultCreateWorkloadOperation extends ComposedOperation implements CreateWorkloadOperation, CreateWorkloadHandle {

    private volatile String workloadId;

    public DefaultCreateWorkloadOperation(String id) {
        super(id);
    }

    @Override
    public CreateWorkloadOperation operation() {
        return this;
    }

    @Override
    public Optional<String> workloadId() {
        return Optional.ofNullable(workloadId);
    }

    @Override
    public void updateProgress(double progress) {
        updateProgressInternal(progress);
    }

    @Override
    public void succeed(String workloadId) {
        this.workloadId = workloadId;
        completeInternal();
    }

    @Override
    public void fail(VirtualizationException cause) {
        failInternal(cause);
    }
}
