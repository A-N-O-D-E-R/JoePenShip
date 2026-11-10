package io.virtualization.sdk.iam;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryApiCredentialRepository implements ApiCredentialRepository {

    private final ConcurrentHashMap<ApiCredentialId, ApiCredential> store = new ConcurrentHashMap<>();

    @Override
    public void save(ApiCredential credential) {
        store.put(credential.id(), credential);
    }

    @Override
    public Optional<ApiCredential> findById(ApiCredentialId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<ApiCredential> findByTokenHash(String tokenHash) {
        return store.values().stream().filter(c -> c.tokenHash().equals(tokenHash)).findFirst();
    }

    @Override
    public List<ApiCredential> findByOwner(UserId ownerId) {
        return store.values().stream().filter(c -> c.ownerId().equals(ownerId)).toList();
    }

    @Override
    public List<ApiCredential> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void delete(ApiCredentialId id) {
        store.remove(id);
    }
}
