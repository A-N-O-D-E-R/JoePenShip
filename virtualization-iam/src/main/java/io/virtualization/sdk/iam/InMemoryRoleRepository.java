package io.virtualization.sdk.iam;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryRoleRepository implements RoleRepository {

    private final ConcurrentHashMap<RoleId, Role> store = new ConcurrentHashMap<>();

    @Override
    public void save(Role role) {
        store.put(role.id(), role);
    }

    @Override
    public Optional<Role> findById(RoleId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Role> findByName(String name) {
        return store.values().stream().filter(r -> r.name().equals(name)).findFirst();
    }

    @Override
    public List<Role> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void delete(RoleId id) {
        store.remove(id);
    }
}
