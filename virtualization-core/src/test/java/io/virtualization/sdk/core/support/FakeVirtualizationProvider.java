package io.virtualization.sdk.core.support;

import io.virtualization.sdk.core.Capability;
import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.ProviderCapabilities;
import io.virtualization.sdk.core.ProviderType;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualMachineState;
import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hand-written test double for {@link VirtualizationProvider}. Serves one canned virtual machine
 * ("vm-1") and enforces the given {@link ProviderCapabilities} exactly like a real provider would.
 */
public final class FakeVirtualizationProvider implements VirtualizationProvider {

    public static final ProviderType TYPE = new ProviderType("fake");

    private final ProviderCapabilities capabilities;
    private final AtomicInteger operationCounter = new AtomicInteger();
    private final VirtualMachine vm =
            new VirtualMachine("vm-1", "test-vm", VirtualMachineState.RUNNING, new ComputeResources(2, 1024));

    public FakeVirtualizationProvider(ProviderCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public ProviderType type() {
        return TYPE;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public List<VirtualMachine> listVirtualMachines() {
        return List.of(vm);
    }

    @Override
    public VirtualMachine getVirtualMachine(String id) {
        if (!vm.id().equals(id)) {
            throw new ResourceNotFoundException("No virtual machine with id '" + id + "'");
        }
        return vm;
    }

    @Override
    public Operation start(String id) {
        return completedOperation(Capability.START);
    }

    @Override
    public Operation stop(String id) {
        return completedOperation(Capability.STOP);
    }

    @Override
    public Operation reboot(String id) {
        return completedOperation(Capability.REBOOT);
    }

    @Override
    public Operation shutdown(String id) {
        return completedOperation(Capability.SHUTDOWN);
    }

    @Override
    public Operation destroy(String id) {
        return completedOperation(Capability.DESTROY);
    }

    private Operation completedOperation(Capability required) {
        if (!capabilities.supports(required)) {
            throw new UnsupportedCapabilityException(
                    "Provider '" + TYPE.id() + "' does not support capability " + required);
        }
        OperationHandle handle = OperationHandle.create("op-" + operationCounter.incrementAndGet());
        handle.complete();
        return handle.operation();
    }
}
