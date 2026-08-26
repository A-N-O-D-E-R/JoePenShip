package io.virtualization.sdk.spring.web.support;

import io.virtualization.sdk.core.Capability;
import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.ProviderCapabilities;
import io.virtualization.sdk.core.ProviderType;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualMachineState;
import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.image.CreateWorkloadHandle;
import io.virtualization.sdk.core.image.CreateWorkloadOperation;
import io.virtualization.sdk.core.image.ImageAvailabilityPolicy;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.WorkloadSpec;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Hand-written test double for {@link VirtualizationProvider}. */
public final class FakeVirtualizationProvider implements VirtualizationProvider {

    public static final ProviderType TYPE = new ProviderType("fake");

    private final ProviderCapabilities capabilities = ProviderCapabilities.of(Capability.START, Capability.STOP);
    private final VirtualMachine vm =
            new VirtualMachine("vm-1", "test-vm", VirtualMachineState.RUNNING, new ComputeResources(2, 1024));
    private final AtomicInteger operationCounter = new AtomicInteger();
    private volatile boolean nextOperationFails;

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
        return completedOperation();
    }

    @Override
    public Operation stop(String id) {
        return completedOperation();
    }

    @Override
    public Operation reboot(String id) {
        return completedOperation();
    }

    @Override
    public Operation shutdown(String id) {
        return completedOperation();
    }

    @Override
    public Operation destroy(String id) {
        return completedOperation();
    }

    @Override
    public CreateWorkloadOperation createFromImage(ImageReference image, WorkloadSpec spec, ImageAvailabilityPolicy policy) {
        CreateWorkloadHandle handle = CreateWorkloadHandle.create("create-" + operationCounter.incrementAndGet());
        if (consumeFailureFlag()) {
            handle.fail(new OperationException("simulated failure"));
        } else {
            handle.succeed(spec.name());
        }
        return handle.operation();
    }

    private Operation completedOperation() {
        OperationHandle handle = OperationHandle.create("op-" + operationCounter.incrementAndGet());
        handle.complete();
        return handle.operation();
    }

    private boolean consumeFailureFlag() {
        boolean fail = nextOperationFails;
        nextOperationFails = false;
        return fail;
    }
}
