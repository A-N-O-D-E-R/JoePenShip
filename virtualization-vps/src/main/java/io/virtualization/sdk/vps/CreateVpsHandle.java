package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.vps.internal.DefaultCreateVpsOperation;

/** The producer side of a {@link CreateVpsOperation}. Mirrors {@link OperationHandle}. */
public interface CreateVpsHandle {

    CreateVpsOperation operation();

    void updateProgress(double progress);

    void succeed(Vps vps);

    void fail(VirtualizationException cause);

    static CreateVpsHandle create(VpsId id) {
        return new DefaultCreateVpsOperation(id);
    }
}
