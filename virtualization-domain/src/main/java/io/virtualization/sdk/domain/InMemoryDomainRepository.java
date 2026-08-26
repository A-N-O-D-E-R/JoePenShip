package io.virtualization.sdk.domain;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryDomainRepository implements DomainRepository {

    private final ConcurrentHashMap<DomainId, Domain> store = new ConcurrentHashMap<>();

    @Override
    public void save(Domain domain) {
        store.put(domain.id(), domain);
    }

    @Override
    public Optional<Domain> findById(DomainId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Domain> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void delete(DomainId id) {
        store.remove(id);
    }
}
