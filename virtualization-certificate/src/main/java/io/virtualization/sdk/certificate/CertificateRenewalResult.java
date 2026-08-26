package io.virtualization.sdk.certificate;

import java.util.Objects;

/**
 * The outcome of one certificate's renewal attempt within a {@link CertificateRenewalScheduler}
 * run — what monitoring inspects to detect a failed renewal (spec: "Expose expiresAt and
 * renewalStatus so monitoring can detect the failure"). {@code error} is {@code null} on success.
 */
public record CertificateRenewalResult(CertificateId certificateId, boolean renewed, String error) {

    public CertificateRenewalResult {
        Objects.requireNonNull(certificateId, "certificateId must not be null");
        if (renewed && error != null) {
            throw new IllegalArgumentException("a successful renewal must not carry an error");
        }
        if (!renewed && error == null) {
            throw new IllegalArgumentException("a failed renewal must carry an error message");
        }
    }

    static CertificateRenewalResult renewed(CertificateId certificateId) {
        return new CertificateRenewalResult(certificateId, true, null);
    }

    static CertificateRenewalResult failed(CertificateId certificateId, String error) {
        return new CertificateRenewalResult(certificateId, false, error);
    }
}
