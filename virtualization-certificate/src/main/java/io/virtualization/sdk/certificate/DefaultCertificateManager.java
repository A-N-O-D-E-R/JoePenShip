package io.virtualization.sdk.certificate;

import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The only {@link CertificateManager} implementation. Multi-provider by design, mirroring {@code
 * DomainManager}: {@code virtualization.certificates.providers.*} config is a named map, and
 * {@link CertificateRequest#issuer()} names one entry per request.
 *
 * <p>Never touches {@link CertificateStore} — that's an {@link AcmeProvider} implementation's own
 * concern (writing {@link CertificateMaterial} as an internal step of fulfilling a request); this
 * class only ever reads/writes {@link Certificate} metadata, via its own {@link
 * CertificateRepository}.
 */
public final class DefaultCertificateManager implements CertificateManager {

    private static final Set<CertificateStatus> RENEWAL_BLOCKED = EnumSet.of(CertificateStatus.REVOKED, CertificateStatus.FAILED);

    private final CertificateRepository repository;
    private final AcmeProviderRegistry acmeProviders;

    public DefaultCertificateManager(CertificateRepository repository, AcmeProviderRegistry acmeProviders) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.acmeProviders = Objects.requireNonNull(acmeProviders, "acmeProviders must not be null");
    }

    @Override
    public Certificate requestCertificate(CertificateRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        AcmeProvider provider = acmeProviders.get(request.issuer());
        CertificateRequestOperation operation = provider.request(request);
        if (operation.await() == OperationStatus.FAILED) {
            throw operation.error().orElseGet(() -> new OperationException("Certificate request failed for issuer '" + request.issuer() + "'"));
        }
        Certificate certificate = operation.certificate()
                .orElseThrow(() -> new OperationException("Certificate request succeeded but returned no certificate"));
        repository.save(certificate);
        return certificate;
    }

    @Override
    public Certificate renew(CertificateId id) {
        Certificate current = get(id);
        if (RENEWAL_BLOCKED.contains(current.status())) {
            throw new IllegalStateException(
                    "Cannot renew certificate '" + id.value() + "' with status " + current.status() + " without explicit recovery");
        }
        Certificate renewed = acmeProviders.get(current.issuer()).renew(current);
        repository.save(renewed);
        return renewed;
    }

    @Override
    public void revoke(CertificateId id) {
        Certificate current = get(id);
        acmeProviders.get(current.issuer()).revoke(current);
        repository.save(new Certificate(
                current.id(), CertificateStatus.REVOKED, current.domains(), current.issuedAt(), current.expiresAt(), current.issuer()));
    }

    @Override
    public Certificate get(CertificateId id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No certificate with id '" + id.value() + "'"));
    }

    @Override
    public List<Certificate> list() {
        return repository.findAll();
    }
}
