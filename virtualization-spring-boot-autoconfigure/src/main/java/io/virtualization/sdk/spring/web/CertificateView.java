package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.certificate.CertificateStatus;

import java.time.Instant;
import java.util.List;

/** Metadata only — {@link Certificate} itself never carries private key material, so there's nothing to accidentally leak here. */
record CertificateView(String id, CertificateStatus status, List<String> domains, Instant issuedAt, Instant expiresAt, String issuer) {

    static CertificateView from(Certificate certificate) {
        return new CertificateView(
                certificate.id().value(), certificate.status(), certificate.domains(), certificate.issuedAt(), certificate.expiresAt(),
                certificate.issuer());
    }
}
