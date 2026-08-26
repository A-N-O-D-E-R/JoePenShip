package io.virtualization.sdk.incus.internal;

import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.incus.client.IncusApiClient;
import io.virtualization.sdk.incus.client.dto.OperationStatusDto;

import java.time.Duration;

/**
 * Drives an {@link OperationHandle} to completion using Incus's long-polling
 * {@code /operations/{id}/wait?timeout=N} endpoint, on a virtual thread. Each call blocks
 * server-side until the operation finishes or {@code waitTimeout} elapses, so — unlike Proxmox
 * task polling — no client-side sleep is needed between calls.
 */
public final class OperationWaiter {

    private OperationWaiter() {}

    public static void waitAsync(IncusApiClient client, String operationId, OperationHandle handle, Duration waitTimeout) {
        Thread.ofVirtual()
                .name("incus-operation-" + operationId)
                .start(() -> waitLoop(client, operationId, handle, waitTimeout));
    }

    private static void waitLoop(IncusApiClient client, String operationId, OperationHandle handle, Duration waitTimeout) {
        try {
            while (true) {
                OperationStatusDto status = client.getSingle(
                        "/operations/" + operationId + "/wait?timeout=" + waitTimeout.toSeconds(), OperationStatusDto.class);
                if (status.isFinished()) {
                    if (status.isSuccessful()) {
                        handle.complete();
                    } else {
                        handle.fail(new OperationException("Incus operation " + operationId + " failed: " + status.err()));
                    }
                    return;
                }
            }
        } catch (VirtualizationException e) {
            handle.fail(e);
        }
    }
}
