package io.virtualization.sdk.network;

import java.util.List;
import java.util.Optional;

public interface IpAllocationRepository {
    void save(IpAllocation allocation);
    Optional<IpAllocation> findById(IpAllocationId id);
    List<IpAllocation> findBySubnet(SubnetId subnetId);
    Optional<IpAllocation> findByAddress(SubnetId subnetId, String address);
    List<IpAllocation> findAll();
    void delete(IpAllocationId id);
}
