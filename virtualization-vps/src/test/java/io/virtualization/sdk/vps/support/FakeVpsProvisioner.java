package io.virtualization.sdk.vps.support;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.vps.CreateVpsHandle;
import io.virtualization.sdk.vps.CreateVpsOperation;
import io.virtualization.sdk.vps.DataSize;
import io.virtualization.sdk.vps.NetworkConfiguration;
import io.virtualization.sdk.vps.StorageConfiguration;
import io.virtualization.sdk.vps.Vps;
import io.virtualization.sdk.vps.VpsId;
import io.virtualization.sdk.vps.VpsProvisioner;
import io.virtualization.sdk.vps.VpsSpec;
import io.virtualization.sdk.vps.VpsState;
import io.virtualization.sdk.vps.VpsType;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/** Hand-written test double for {@link VpsProvisioner}. Completes every operation immediately, synchronously. */
public final class FakeVpsProvisioner implements VpsProvisioner {

    private final boolean succeed;
    private final VirtualizationException failureCause;
    private final AtomicInteger createCalls = new AtomicInteger();

    private FakeVpsProvisioner(boolean succeed, VirtualizationException failureCause) {
        this.succeed = succeed;
        this.failureCause = failureCause;
    }

    public static FakeVpsProvisioner succeeding() {
        return new FakeVpsProvisioner(true, null);
    }

    public static FakeVpsProvisioner failing(VirtualizationException cause) {
        return new FakeVpsProvisioner(false, cause);
    }

    /** How many times {@link #create} was actually invoked — for idempotency/concurrency assertions. */
    public int createCallCount() {
        return createCalls.get();
    }

    @Override
    public CreateVpsOperation create(VpsId id, VpsSpec spec) {
        createCalls.incrementAndGet();
        CreateVpsHandle handle = CreateVpsHandle.create(id);
        completeCreate(handle, id, spec.name(), spec.type(), spec.image(), spec);
        return handle.operation();
    }

    @Override
    public CreateVpsOperation rebuild(VpsId id, Vps current, ImageReference image) {
        CreateVpsHandle handle = CreateVpsHandle.create(id);
        completeCreate(handle, id, current.name(), current.type(), image, current.spec());
        return handle.operation();
    }

    @Override
    public Operation start(VpsId id, Vps current) {
        return completedOperation(id);
    }

    @Override
    public Operation stop(VpsId id, Vps current) {
        return completedOperation(id);
    }

    @Override
    public Operation restart(VpsId id, Vps current) {
        return completedOperation(id);
    }

    @Override
    public Operation shutdown(VpsId id, Vps current) {
        return completedOperation(id);
    }

    @Override
    public Operation destroy(VpsId id, Vps current) {
        return completedOperation(id);
    }

    private Operation completedOperation(VpsId id) {
        OperationHandle handle = OperationHandle.create(id.value());
        if (succeed) {
            handle.complete();
        } else {
            handle.fail(failureCause);
        }
        return handle.operation();
    }

    private void completeCreate(CreateVpsHandle handle, VpsId id, String name, VpsType type, ImageReference image, VpsSpec spec) {
        if (!succeed) {
            handle.fail(failureCause);
            return;
        }
        Instant now = Instant.now();
        Vps vps = new Vps(
                id, name, VpsState.READY, type, image, new ComputeResources(1, 1_024),
                new StorageConfiguration(DataSize.ofGigabytes(10)), NetworkConfiguration.UNSPECIFIED, spec, "fake",
                "default", "workload-" + id.value(), now, now, null, null, null);
        handle.succeed(vps);
    }
}
