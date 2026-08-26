package io.sablier.core;

import io.sablier.core.exception.SablierException;
import io.sablier.core.internal.DefaultOperation;

/**
 * The producer side of an asynchronous {@link Operation}, used by provider modules to drive an
 * operation to completion from whatever async mechanism they use internally.
 *
 * <p>Kept separate from {@link Operation} so consumers of the SDK only ever see the read-only
 * view; only code holding the handle can mutate it.
 */
public interface OperationHandle {

    /** The read-only view of this operation, to hand to callers. */
    Operation operation();

    /**
     * Reports progress as a fraction between 0.0 and 1.0.
     *
     * @throws IllegalArgumentException if progress is outside {@code [0.0, 1.0]}
     */
    void updateProgress(double progress);

    /** Marks the operation as successfully completed. */
    void complete();

    /** Marks the operation as failed with the given cause. */
    void fail(SablierException cause);

    static OperationHandle create(String id) {
        return new DefaultOperation(id);
    }
}
