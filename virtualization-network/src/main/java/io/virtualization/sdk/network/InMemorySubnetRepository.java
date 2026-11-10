package io.virtualization.sdk.network;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemorySubnetRepository implements SubnetRepository {

    private final ConcurrentHashMap<SubnetId, Subnet> store = new ConcurrentHashMap<>();

    @Override
    public void save(Subnet subnet) {
        store.put(subnet.id(), subnet);
    }

    @Override
    public Optional<Subnet> findById(SubnetId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Subnet> findByNetwork(NetworkId networkId) {
        return store.values().stream().filter(s -> s.networkId().equals(networkId)).toList();
    }

    @Override
    public List<Subnet> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void delete(SubnetId id) {
        store.remove(id);
    }
}
