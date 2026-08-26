package io.virtualization.sdk.vps.internal;

import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.vps.CreateVpsHandle;
import io.virtualization.sdk.vps.CreateVpsOperation;
import io.virtualization.sdk.vps.Vps;
import io.virtualization.sdk.vps.VpsId;

import java.util.Optional;

/** Not part of the public API — obtain instances via {@link CreateVpsHandle#create(VpsId)}. */
public final class DefaultCreateVpsOperation extends ComposedOperation implements CreateVpsOperation, CreateVpsHandle {

    private final VpsId vpsId;
    private volatile Vps vps;

    public DefaultCreateVpsOperation(VpsId vpsId) {
        super(vpsId.value());
        this.vpsId = vpsId;
    }

    @Override
    public CreateVpsOperation operation() {
        return this;
    }

    @Override
    public VpsId vpsId() {
        return vpsId;
    }

    @Override
    public Optional<Vps> vps() {
        return Optional.ofNullable(vps);
    }

    @Override
    public void updateProgress(double progress) {
        updateProgressInternal(progress);
    }

    @Override
    public void succeed(Vps vps) {
        this.vps = vps;
        completeInternal();
    }

    @Override
    public void fail(VirtualizationException cause) {
        failInternal(cause);
    }
}
