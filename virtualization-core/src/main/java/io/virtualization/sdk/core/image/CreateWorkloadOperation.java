package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.VirtualizationProvider;

import java.util.Optional;

/**
 * A read-only view of an asynchronous {@link VirtualizationProvider#createFromImage} in progress.
 */
public interface CreateWorkloadOperation extends Operation {

    /** The id of the created workload (VM or container), populated once this reaches {@link OperationStatus#SUCCEEDED}. */
    Optional<String> workloadId();
}
