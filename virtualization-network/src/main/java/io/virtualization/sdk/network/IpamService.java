package io.virtualization.sdk.network;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.util.List;

public interface IpamService {

    /**
     * @throws ResourceNotFoundException if {@code subnetId} is unknown
     * @throws UnsupportedOperationException if the subnet's CIDR is IPv6
     * @throws IllegalStateException if the subnet has no free address left
     */
    IpAllocation allocate(SubnetId subnetId, String ownerRef);

    /** A no-op if {@code id} is unknown or already released. */
    void release(IpAllocationId id);

    List<IpAllocation> list(SubnetId subnetId);
}
