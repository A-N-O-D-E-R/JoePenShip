package io.virtualization.sdk.iam;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryGroupRepository implements GroupRepository {

    private final ConcurrentHashMap<GroupId, Group> store = new ConcurrentHashMap<>();

    @Override
    public void save(Group group) {
        store.put(group.id(), group);
    }

    @Override
    public Optional<Group> findById(GroupId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Group> findByName(String name) {
        return store.values().stream().filter(g -> g.name().equals(name)).findFirst();
    }

    @Override
    public List<Group> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void delete(GroupId id) {
        store.remove(id);
    }
}
