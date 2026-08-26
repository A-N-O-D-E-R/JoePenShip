package io.virtualization.sdk.certificate;

import java.util.Optional;

/**
 * Persists {@link CertificateMaterial} — private keys are highly sensitive: never log, never put
 * in a normal database field alongside {@link Certificate} metadata, never return from a CLI/API
 * response by default. {@link InMemoryCertificateStore} is this phase's only implementation (spec:
 * "in-memory... for tests"); a production deployment needs a real secret-storage backend (Vault,
 * a cloud secrets manager, an encrypted filesystem — none implemented yet).
 */
public interface CertificateStore {
    void store(CertificateId id, CertificateMaterial material);
    Optional<CertificateMaterial> load(CertificateId id);
    void delete(CertificateId id);
}
