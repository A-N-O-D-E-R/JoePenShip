package io.virtualization.sdk.network;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** The only {@link IpamService} implementation. */
public final class DefaultIpamService implements IpamService {

    private final SubnetRepository subnetRepository;
    private final IpAllocationRepository allocationRepository;

    public DefaultIpamService(SubnetRepository subnetRepository, IpAllocationRepository allocationRepository) {
        this.subnetRepository = Objects.requireNonNull(subnetRepository, "subnetRepository must not be null");
        this.allocationRepository = Objects.requireNonNull(allocationRepository, "allocationRepository must not be null");
    }

    @Override
    public IpAllocation allocate(SubnetId subnetId, String ownerRef) {
        Objects.requireNonNull(subnetId, "subnetId must not be null");
        Objects.requireNonNull(ownerRef, "ownerRef must not be null");
        Subnet subnet = subnetRepository.findById(subnetId)
                .orElseThrow(() -> new ResourceNotFoundException("No subnet with id '" + subnetId.value() + "'"));

        Set<String> taken = new HashSet<>();
        for (IpAllocation existing : allocationRepository.findBySubnet(subnetId)) {
            taken.add(existing.address());
        }
        if (subnet.gateway() != null) {
            taken.add(subnet.gateway());
        }

        Iterator<String> candidates = subnet.cidrBlock().usableHostAddresses();
        while (candidates.hasNext()) {
            String address = candidates.next();
            if (!taken.contains(address)) {
                IpAllocation allocation = new IpAllocation(IpAllocationId.generate(), subnetId, address, ownerRef, Instant.now());
                allocationRepository.save(allocation);
                return allocation;
            }
        }
        throw new IllegalStateException("subnet '" + subnetId.value() + "' is exhausted — no free address left");
    }

    @Override
    public void release(IpAllocationId id) {
        Objects.requireNonNull(id, "id must not be null");
        allocationRepository.delete(id);
    }

    @Override
    public List<IpAllocation> list(SubnetId subnetId) {
        Objects.requireNonNull(subnetId, "subnetId must not be null");
        return allocationRepository.findBySubnet(subnetId);
    }
}
