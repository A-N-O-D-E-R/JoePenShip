package io.virtualization.sdk.proxmox.internal;

import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.proxmox.client.ProxmoxApiClient;
import io.virtualization.sdk.proxmox.client.dto.TaskStatusDto;

import java.time.Duration;

/**
 * Drives an {@link OperationHandle} to completion by polling a Proxmox task's status on a virtual
 * thread until it finishes, fails, or the client raises a provider exception.
 */
public final class TaskPoller {

    private TaskPoller() {}

    public static void pollAsync(
            ProxmoxApiClient client, String node, String upid, OperationHandle handle, Duration pollInterval) {
        Thread.ofVirtual().name("proxmox-task-" + upid).start(() -> poll(client, node, upid, handle, pollInterval));
    }

    private static void poll(ProxmoxApiClient client, String node, String upid, OperationHandle handle, Duration pollInterval) {
        try {
            while (true) {
                TaskStatusDto status = client.getSingle("/nodes/" + node + "/tasks/" + upid + "/status", TaskStatusDto.class);
                if (status.isFinished()) {
                    if (status.isSuccessful()) {
                        handle.complete();
                    } else {
                        handle.fail(new OperationException("Proxmox task " + upid + " failed: " + status.exitstatus()));
                    }
                    return;
                }
                Thread.sleep(pollInterval);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handle.fail(new OperationException("Interrupted while polling Proxmox task " + upid, e));
        } catch (VirtualizationException e) {
            handle.fail(e);
        }
    }
}
