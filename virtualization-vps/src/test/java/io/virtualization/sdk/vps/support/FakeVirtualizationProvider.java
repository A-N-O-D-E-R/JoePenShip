package io.virtualization.sdk.vps.support;

import io.virtualization.sdk.core.Capability;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.ProviderCapabilities;
import io.virtualization.sdk.core.ProviderType;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.image.CreateWorkloadHandle;
import io.virtualization.sdk.core.image.CreateWorkloadOperation;
import io.virtualization.sdk.core.image.ImageAvailabilityPolicy;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.WorkloadSpec;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Hand-written test double for {@link VirtualizationProvider}, for {@code DefaultVpsProvisioner} tests. */
public final class FakeVirtualizationProvider implements VirtualizationProvider {

    public static final ProviderType TYPE = new ProviderType("fake");

    private final AtomicInteger workloadCounter = new AtomicInteger();
    private final List<String> calls = new CopyOnWriteArrayList<>();
    private volatile boolean createFails;
    private volatile boolean lifecycleFails;
    private volatile WorkloadSpec lastWorkloadSpec;
    private volatile ImageReference lastImageReference;

    public void createFails() {
        this.createFails = true;
    }

    public void lifecycleFails() {
        this.lifecycleFails = true;
    }

    /** Recorded calls, e.g. {@code "createFromImage:web-01"}, {@code "start:workload-1"}. */
    public List<String> calls() {
        return calls;
    }

    public WorkloadSpec lastWorkloadSpec() {
        return lastWorkloadSpec;
    }

    public ImageReference lastImageReference() {
        return lastImageReference;
    }

    @Override
    public ProviderType type() {
        return TYPE;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return ProviderCapabilities.of(Capability.START, Capability.STOP, Capability.REBOOT, Capability.SHUTDOWN, Capability.DESTROY);
    }

    @Override
    public List<VirtualMachine> listVirtualMachines() {
        return List.of();
    }

    @Override
    public VirtualMachine getVirtualMachine(String id) {
        throw new ResourceNotFoundException("No virtual machine with id '" + id + "'");
    }

    @Override
    public Operation start(String id) {
        return dispatch("start", id);
    }

    @Override
    public Operation stop(String id) {
        return dispatch("stop", id);
    }

    @Override
    public Operation reboot(String id) {
        return dispatch("reboot", id);
    }

    @Override
    public Operation shutdown(String id) {
        return dispatch("shutdown", id);
    }

    @Override
    public Operation destroy(String id) {
        return dispatch("destroy", id);
    }

    @Override
    public CreateWorkloadOperation createFromImage(ImageReference image, WorkloadSpec spec, ImageAvailabilityPolicy policy) {
        lastImageReference = image;
        lastWorkloadSpec = spec;
        calls.add("createFromImage:" + spec.name());
        String workloadId = "workload-" + workloadCounter.incrementAndGet();
        CreateWorkloadHandle handle = CreateWorkloadHandle.create("create-" + workloadId);
        if (createFails) {
            handle.fail(new OperationException("simulated create failure"));
        } else {
            handle.succeed(workloadId);
        }
        return handle.operation();
    }

    private Operation dispatch(String action, String id) {
        calls.add(action + ":" + id);
        OperationHandle handle = OperationHandle.create(action + "-" + id);
        if (lifecycleFails) {
            handle.fail(new OperationException("simulated " + action + " failure"));
        } else {
            handle.complete();
        }
        return handle.operation();
    }
}
