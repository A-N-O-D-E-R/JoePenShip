package io.virtualization.sdk.certificate;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCertificateStore implements CertificateStore {

    private final ConcurrentHashMap<CertificateId, CertificateMaterial> store = new ConcurrentHashMap<>();

    @Override
    public void store(CertificateId id, CertificateMaterial material) {
        store.put(id, material);
    }

    @Override
    public Optional<CertificateMaterial> load(CertificateId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void delete(CertificateId id) {
        store.remove(id);
    }
}
