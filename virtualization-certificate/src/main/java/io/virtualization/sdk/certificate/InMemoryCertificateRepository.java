package io.virtualization.sdk.certificate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCertificateRepository implements CertificateRepository {

    private final ConcurrentHashMap<CertificateId, Certificate> store = new ConcurrentHashMap<>();

    @Override
    public void save(Certificate certificate) {
        store.put(certificate.id(), certificate);
    }

    @Override
    public Optional<Certificate> findById(CertificateId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Certificate> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void delete(CertificateId id) {
        store.remove(id);
    }
}
