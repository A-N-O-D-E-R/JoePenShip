package io.virtualization.sdk.qemu;

import io.virtualization.sdk.core.Capability;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.ProviderCapabilities;
import io.virtualization.sdk.core.ProviderType;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.qemu.internal.DomainMapper;
import io.virtualization.sdk.qemu.qmp.QmpClient;

import java.util.List;

/**
 * {@link VirtualizationProvider} backed by a single running QEMU process, controlled over its QMP
 * socket.
 *
 * <p>A QMP socket has no concept of a VM catalog or a "destroy" operation — it controls one
 * already-running process. {@link #listVirtualMachines()} always returns exactly the one VM this
 * provider was configured for, and {@link #destroy} is unsupported ({@link
 * ProviderCapabilities} omits {@link Capability#DESTROY}): tearing down the process is outside
 * what QMP itself exposes.
 *
 * <p>{@link #start}, {@link #stop}, {@link #reboot} and {@link #shutdown} map to QMP's {@code
 * cont}, {@code stop}, {@code system_reset} and {@code system_powerdown} respectively. The
 * returned {@link Operation} completes as soon as QEMU acknowledges the command over QMP — it
 * does not wait for a corresponding guest-level event (e.g. the guest actually finishing an ACPI
 * shutdown), since a guest may never respond to that request at all.
 */
public final class QemuProvider implements VirtualizationProvider {

    public static final ProviderType TYPE = new ProviderType("qemu");

    private static final ProviderCapabilities CAPABILITIES =
            ProviderCapabilities.of(Capability.START, Capability.STOP, Capability.REBOOT, Capability.SHUTDOWN);

    private final String vmId;
    private final QmpClient client;

    public QemuProvider(QemuClientConfig config) {
        this(config.vmId(), QmpClient.connect(config.endpoint(), config.connectTimeout(), config.commandTimeout()));
    }

    /** Visible for tests, to inject a client talking to a fake QMP server. */
    QemuProvider(String vmId, QmpClient client) {
        this.vmId = vmId;
        this.client = client;
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
        return List.of(currentVirtualMachine());
    }

    @Override
    public VirtualMachine getVirtualMachine(String id) {
        requireKnownVmId(id);
        return currentVirtualMachine();
    }

    @Override
    public Operation start(String id) {
        return dispatch(id, "cont", Capability.START);
    }

    @Override
    public Operation stop(String id) {
        return dispatch(id, "stop", Capability.STOP);
    }

    @Override
    public Operation reboot(String id) {
        return dispatch(id, "system_reset", Capability.REBOOT);
    }

    @Override
    public Operation shutdown(String id) {
        return dispatch(id, "system_powerdown", Capability.SHUTDOWN);
    }

    @Override
    public Operation destroy(String id) {
        throw new UnsupportedCapabilityException(
                "Provider '" + TYPE.id() + "' does not support capability DESTROY: QMP has no VM-teardown command");
    }

    private Operation dispatch(String id, String qmpCommand, Capability required) {
        requireKnownVmId(id);
        if (!CAPABILITIES.supports(required)) {
            throw new UnsupportedCapabilityException("Provider '" + TYPE.id() + "' does not support capability " + required);
        }
        OperationHandle handle = OperationHandle.create(qmpCommand + "-" + System.nanoTime());
        try {
            client.execute(qmpCommand);
            handle.complete();
        } catch (VirtualizationException e) {
            handle.fail(e);
        }
        return handle.operation();
    }

    private VirtualMachine currentVirtualMachine() {
        return DomainMapper.toVirtualMachine(vmId, client.execute("query-status"), client.execute("query-cpus"));
    }

    private void requireKnownVmId(String id) {
        if (!vmId.equals(id)) {
            throw new ResourceNotFoundException("No QEMU virtual machine with id '" + id + "' (this provider manages '" + vmId + "')");
        }
    }
}
