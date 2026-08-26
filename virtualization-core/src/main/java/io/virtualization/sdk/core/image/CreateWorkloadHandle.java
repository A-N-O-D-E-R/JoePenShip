package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.core.image.internal.DefaultCreateWorkloadOperation;

/**
 * The producer side of a {@link CreateWorkloadOperation}, used by provider modules to drive
 * workload creation to completion. Mirrors {@link OperationHandle}.
 */
public interface CreateWorkloadHandle {

    /** The read-only view of this operation, to hand to SDK consumers. */
    CreateWorkloadOperation operation();

    /**
     * Reports progress as a fraction between 0.0 and 1.0.
     *
     * @throws IllegalArgumentException if progress is outside {@code [0.0, 1.0]}
     */
    void updateProgress(double progress);

    /** Marks the creation as successfully completed, with the id of the created workload. */
    void succeed(String workloadId);

    /** Marks the creation as failed with the given cause. */
    void fail(VirtualizationException cause);

    static CreateWorkloadHandle create(String id) {
        return new DefaultCreateWorkloadOperation(id);
    }
}
