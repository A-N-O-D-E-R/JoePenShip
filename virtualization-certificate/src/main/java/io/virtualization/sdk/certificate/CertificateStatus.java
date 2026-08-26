package io.virtualization.sdk.certificate;

public enum CertificateStatus {
    REQUESTED,
    PENDING,
    ACTIVE,
    EXPIRING,
    EXPIRED,
    REVOKED,
    FAILED
}
