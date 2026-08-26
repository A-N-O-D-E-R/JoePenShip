package io.virtualization.sdk.certificate;

import java.util.List;
import java.util.Optional;

/** Persists {@link Certificate} metadata rows. {@link InMemoryCertificateRepository} is this phase's only implementation. */
public interface CertificateRepository {
    void save(Certificate certificate);
    Optional<Certificate> findById(CertificateId id);
    List<Certificate> findAll();
    void delete(CertificateId id);
}
