package io.virtualization.sdk.certificate;

import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.util.List;

public interface CertificateManager {

    /**
     * Synchronous facade over {@link AcmeProvider#request} — awaits the resulting operation
     * internally, no ACME protocol logic here.
     *
     * @throws ConfigurationException if {@code request.issuer()} isn't a registered ACME provider
     * @throws OperationException     if the request fails
     */
    Certificate requestCertificate(CertificateRequest request);

    /**
     * @throws ResourceNotFoundException if {@code id} is unknown
     * @throws IllegalStateException     if the certificate's current status is {@code REVOKED} or
     *                                    {@code FAILED} — renewal needs explicit recovery, not a
     *                                    blind retry
     */
    Certificate renew(CertificateId id);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    void revoke(CertificateId id);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    Certificate get(CertificateId id);

    List<Certificate> list();
}
