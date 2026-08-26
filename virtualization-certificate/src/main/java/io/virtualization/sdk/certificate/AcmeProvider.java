package io.virtualization.sdk.certificate;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

/**
 * An ACME (or ACME-like) certificate authority (Let's Encrypt, ZeroSSL, an internal CA).
 * Implementations live in their own provider modules — this module never depends on one.
 *
 * <p>{@link #request} is async (real issuance can take real time waiting on challenge
 * validation); {@link #renew}/{@link #revoke} stay synchronous — a deliberate asymmetry, not an
 * oversight.
 */
public interface AcmeProvider {

    CertificateRequestOperation request(CertificateRequest request);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    Certificate get(CertificateId id);

    void revoke(Certificate current);

    Certificate renew(Certificate current);
}
