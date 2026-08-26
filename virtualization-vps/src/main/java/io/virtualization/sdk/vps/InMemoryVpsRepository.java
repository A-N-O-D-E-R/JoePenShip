package io.virtualization.sdk.vps;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryVpsRepository implements VpsRepository {

    private final ConcurrentHashMap<VpsId, Vps> store = new ConcurrentHashMap<>();

    @Override
    public void save(Vps vps) {
        store.put(vps.id(), vps);
    }

    @Override
    public Optional<Vps> findById(VpsId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Vps> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void delete(VpsId id) {
        store.remove(id);
    }
}
