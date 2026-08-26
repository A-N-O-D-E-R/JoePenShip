package io.virtualization.sdk.cli.support;

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
import io.virtualization.sdk.core.image.CreateWorkloadHandle;
import io.virtualization.sdk.core.image.CreateWorkloadOperation;
import io.virtualization.sdk.core.image.ImageAvailabilityPolicy;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.WorkloadSpec;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Hand-written test double, mirroring the one in {@code virtualization-core}'s own tests (not reusable across modules). */
public final class FakeVirtualizationProvider implements VirtualizationProvider {

    public static final ProviderType TYPE = new ProviderType("fake");

    private final ProviderCapabilities capabilities;
    private final VirtualMachine vm;
    private final AtomicInteger operationCounter = new AtomicInteger();
    private volatile boolean nextOperationFails;

    public FakeVirtualizationProvider() {
        this(ProviderCapabilities.of(Capability.START, Capability.STOP, Capability.REBOOT, Capability.SHUTDOWN, Capability.DESTROY));
    }

    public FakeVirtualizationProvider(ProviderCapabilities capabilities) {
        this.capabilities = capabilities;
        this.vm = new VirtualMachine("vm-1", "test-vm", VirtualMachineState.RUNNING, new ComputeResources(2, 2048));
    }

    public void nextOperationFails() {
        this.nextOperationFails = true;
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
        return dispatch(Capability.START);
    }

    @Override
    public Operation stop(String id) {
        return dispatch(Capability.STOP);
    }

    @Override
    public Operation reboot(String id) {
        return dispatch(Capability.REBOOT);
    }

    @Override
    public Operation shutdown(String id) {
        return dispatch(Capability.SHUTDOWN);
    }

    @Override
    public Operation destroy(String id) {
        return dispatch(Capability.DESTROY);
    }

    @Override
    public CreateWorkloadOperation createFromImage(ImageReference image, WorkloadSpec spec, ImageAvailabilityPolicy policy) {
        CreateWorkloadHandle handle = CreateWorkloadHandle.create("create-" + operationCounter.incrementAndGet());
        boolean fail = nextOperationFails;
        nextOperationFails = false;
        if (fail) {
            handle.fail(new io.virtualization.sdk.core.exception.OperationException("simulated failure"));
        } else {
            handle.succeed(spec.name());
        }
        return handle.operation();
    }

    private Operation dispatch(Capability required) {
        if (!capabilities.supports(required)) {
            throw new UnsupportedCapabilityException("Provider '" + TYPE.id() + "' does not support capability " + required);
        }
        OperationHandle handle = OperationHandle.create("op-" + operationCounter.incrementAndGet());
        boolean fail = nextOperationFails;
        nextOperationFails = false;
        if (fail) {
            handle.fail(new io.virtualization.sdk.core.exception.OperationException("simulated failure"));
        } else {
            handle.complete();
        }
        return handle.operation();
    }
}
