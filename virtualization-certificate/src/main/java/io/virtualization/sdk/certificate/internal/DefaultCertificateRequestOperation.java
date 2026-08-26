package io.virtualization.sdk.certificate.internal;

import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.certificate.CertificateId;
import io.virtualization.sdk.certificate.CertificateRequestHandle;
import io.virtualization.sdk.certificate.CertificateRequestOperation;
import io.virtualization.sdk.core.exception.VirtualizationException;

import java.util.Optional;

/** Not part of the public API — obtain instances via {@link CertificateRequestHandle#create(CertificateId)}. */
public final class DefaultCertificateRequestOperation extends ComposedOperation implements CertificateRequestOperation, CertificateRequestHandle {

    private final CertificateId certificateId;
    private volatile Certificate certificate;

    public DefaultCertificateRequestOperation(CertificateId certificateId) {
        super(certificateId.value());
        this.certificateId = certificateId;
    }

    @Override
    public CertificateRequestOperation operation() {
        return this;
    }

    @Override
    public CertificateId certificateId() {
        return certificateId;
    }

    @Override
    public Optional<Certificate> certificate() {
        return Optional.ofNullable(certificate);
    }

    @Override
    public void updateProgress(double progress) {
        updateProgressInternal(progress);
    }

    @Override
    public void succeed(Certificate certificate) {
        this.certificate = certificate;
        completeInternal();
    }

    @Override
    public void fail(VirtualizationException cause) {
        failInternal(cause);
    }
}
