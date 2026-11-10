package io.virtualization.sdk.network;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryNetworkRepository implements NetworkRepository {

    private final ConcurrentHashMap<NetworkId, Network> store = new ConcurrentHashMap<>();

    @Override
    public void save(Network network) {
        store.put(network.id(), network);
    }

    @Override
    public Optional<Network> findById(NetworkId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Network> findByName(String name) {
        return store.values().stream().filter(n -> n.name().equals(name)).findFirst();
    }

    @Override
    public List<Network> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void delete(NetworkId id) {
        store.remove(id);
    }
}
