package io.virtualization.sdk.certificate;

import io.virtualization.sdk.certificate.internal.DefaultCertificateRequestOperation;
import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.exception.VirtualizationException;

/** The producer side of a {@link CertificateRequestOperation}. Mirrors {@link OperationHandle}. */
public interface CertificateRequestHandle {

    CertificateRequestOperation operation();

    void updateProgress(double progress);

    void succeed(Certificate certificate);

    void fail(VirtualizationException cause);

    static CertificateRequestHandle create(CertificateId id) {
        return new DefaultCertificateRequestOperation(id);
    }
}
