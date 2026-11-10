package io.virtualization.sdk.incus.internal;

import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.incus.client.IncusApiClient;
import io.virtualization.sdk.incus.client.dto.OperationStatusDto;

import java.time.Duration;

/**
 * Blocks the calling thread until an Incus background operation finishes, for callers whose own
 * contract is synchronous (e.g. {@code StorageProvider}) rather than {@code Operation}-returning
 * — unlike {@link OperationWaiter}, which drives an {@code OperationHandle} on a virtual thread.
 */
public final class SyncOperationWaiter {

    private SyncOperationWaiter() {}

    public static void await(IncusApiClient client, String operationId, Duration waitTimeout) {
        while (true) {
            OperationStatusDto status = client.getSingle(
                    "/operations/" + operationId + "/wait?timeout=" + waitTimeout.toSeconds(), OperationStatusDto.class);
            if (status.isFinished()) {
                if (!status.isSuccessful()) {
                    throw new OperationException("Incus operation " + operationId + " failed: " + status.err());
                }
                return;
            }
        }
    }
}
