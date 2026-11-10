package io.virtualization.sdk.network;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryIpAllocationRepository implements IpAllocationRepository {

    private final ConcurrentHashMap<IpAllocationId, IpAllocation> store = new ConcurrentHashMap<>();

    @Override
    public void save(IpAllocation allocation) {
        store.put(allocation.id(), allocation);
    }

    @Override
    public Optional<IpAllocation> findById(IpAllocationId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<IpAllocation> findBySubnet(SubnetId subnetId) {
        return store.values().stream().filter(a -> a.subnetId().equals(subnetId)).toList();
    }

    @Override
    public Optional<IpAllocation> findByAddress(SubnetId subnetId, String address) {
        return store.values().stream()
                .filter(a -> a.subnetId().equals(subnetId) && a.address().equals(address))
                .findFirst();
    }

    @Override
    public List<IpAllocation> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void delete(IpAllocationId id) {
        store.remove(id);
    }
}
