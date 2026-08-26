package io.virtualization.sdk.proxmox;

import io.virtualization.sdk.core.Capability;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.ProviderCapabilities;
import io.virtualization.sdk.core.ProviderType;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.proxmox.client.ProxmoxApiClient;
import io.virtualization.sdk.proxmox.client.dto.ClusterResourceDto;
import io.virtualization.sdk.proxmox.internal.DomainMapper;
import io.virtualization.sdk.proxmox.internal.TaskPoller;

import java.time.Duration;
import java.util.List;

/**
 * {@link VirtualizationProvider} backed by the Proxmox VE HTTPS REST API.
 *
 * <p>Covers QEMU virtual machines only in this iteration; LXC containers, snapshots, storage and
 * networks are exposed by the Proxmox REST API and mapped by {@code virtualization-core}'s domain
 * model, but {@link VirtualizationProvider} itself has no methods for them yet — extending that
 * interface (or adding provider-specific accessor methods) is a follow-up, not implemented here to
 * avoid API surface with no caller.
 */
public final class ProxmoxProvider implements VirtualizationProvider {

    public static final ProviderType TYPE = new ProviderType("proxmox");

    private static final ProviderCapabilities CAPABILITIES = ProviderCapabilities.of(
            Capability.START, Capability.STOP, Capability.REBOOT, Capability.SHUTDOWN, Capability.DESTROY);

    private final ProxmoxApiClient client;
    private final Duration taskPollInterval;

    public ProxmoxProvider(ProxmoxClientConfig config) {
        this(new ProxmoxApiClient(config), config.taskPollInterval());
    }

    /** Visible for tests, to inject a client talking to a fake server. */
    ProxmoxProvider(ProxmoxApiClient client, Duration taskPollInterval) {
        this.client = client;
        this.taskPollInterval = taskPollInterval;
    }

    @Override
    public ProviderType type() {
        return TYPE;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public List<VirtualMachine> listVirtualMachines() {
        return client.getList("/cluster/resources?type=vm", ClusterResourceDto.class).stream()
                .filter(ClusterResourceDto::isQemuVm)
                .map(DomainMapper::toVirtualMachine)
                .toList();
    }

    @Override
    public VirtualMachine getVirtualMachine(String id) {
        return DomainMapper.toVirtualMachine(findResource(id));
    }

    @Override
    public Operation start(String id) {
        return dispatch(id, "start", Capability.START);
    }

    @Override
    public Operation stop(String id) {
        return dispatch(id, "stop", Capability.STOP);
    }

    @Override
    public Operation reboot(String id) {
        return dispatch(id, "reboot", Capability.REBOOT);
    }

    @Override
    public Operation shutdown(String id) {
        return dispatch(id, "shutdown", Capability.SHUTDOWN);
    }

    @Override
    public Operation destroy(String id) {
        if (!CAPABILITIES.supports(Capability.DESTROY)) {
            throw new UnsupportedCapabilityException("Provider '" + TYPE.id() + "' does not support capability DESTROY");
        }
        ClusterResourceDto resource = findResource(id);
        String upid = client.deleteForTaskId("/nodes/" + resource.node() + "/qemu/" + id);
        return awaitedOperation(resource.node(), upid);
    }

    private Operation dispatch(String id, String action, Capability required) {
        if (!CAPABILITIES.supports(required)) {
            throw new UnsupportedCapabilityException("Provider '" + TYPE.id() + "' does not support capability " + required);
        }
        ClusterResourceDto resource = findResource(id);
        String upid = client.postForTaskId("/nodes/" + resource.node() + "/qemu/" + id + "/status/" + action);
        return awaitedOperation(resource.node(), upid);
    }

    private Operation awaitedOperation(String node, String upid) {
        OperationHandle handle = OperationHandle.create(upid);
        TaskPoller.pollAsync(client, node, upid, handle, taskPollInterval);
        return handle.operation();
    }

    private ClusterResourceDto findResource(String id) {
        return client.getList("/cluster/resources?type=vm", ClusterResourceDto.class).stream()
                .filter(ClusterResourceDto::isQemuVm)
                .filter(dto -> id.equals(String.valueOf(dto.vmid())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No Proxmox QEMU VM with id '" + id + "'"));
    }
}
