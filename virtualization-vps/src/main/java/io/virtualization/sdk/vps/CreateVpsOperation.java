package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;

import java.util.Optional;

/** A read-only view of an asynchronous {@link VpsManager#create}/{@link VpsManager#rebuild} in progress. */
public interface CreateVpsOperation extends Operation {

    /** Known immediately — generated before provisioning starts, unlike a provider-assigned id. */
    VpsId vpsId();

    /** The resulting {@link Vps}, populated once this reaches {@link OperationStatus#SUCCEEDED}. */
    Optional<Vps> vps();
}
