package io.virtualization.sdk.spring.web.support;

import io.virtualization.sdk.certificate.AcmeProvider;
import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.certificate.CertificateId;
import io.virtualization.sdk.certificate.CertificateRequest;
import io.virtualization.sdk.certificate.CertificateRequestHandle;
import io.virtualization.sdk.certificate.CertificateRequestOperation;
import io.virtualization.sdk.certificate.CertificateStatus;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Hand-written test double for {@link AcmeProvider}, independently duplicated per this repo's "no test-jar sharing" precedent. */
public final class FakeAcmeProvider implements AcmeProvider {

    private static final Duration VALIDITY = Duration.ofDays(90);

    private final Map<CertificateId, Certificate> issued = new ConcurrentHashMap<>();

    @Override
    public CertificateRequestOperation request(CertificateRequest request) {
        CertificateRequestHandle handle = CertificateRequestHandle.create(CertificateId.generate());
        Instant now = Instant.now();
        Certificate certificate = new Certificate(
                handle.operation().certificateId(), CertificateStatus.ACTIVE, request.domains(), now, now.plus(VALIDITY), request.issuer());
        issued.put(certificate.id(), certificate);
        handle.succeed(certificate);
        return handle.operation();
    }

    @Override
    public Certificate get(CertificateId id) {
        return requireIssued(id);
    }

    @Override
    public void revoke(Certificate current) {
        issued.put(current.id(), new Certificate(
                current.id(), CertificateStatus.REVOKED, current.domains(), current.issuedAt(), current.expiresAt(), current.issuer()));
    }

    @Override
    public Certificate renew(Certificate current) {
        Instant now = Instant.now();
        Certificate renewed = new Certificate(
                current.id(), CertificateStatus.ACTIVE, current.domains(), now, now.plus(VALIDITY), current.issuer());
        issued.put(current.id(), renewed);
        return renewed;
    }

    private Certificate requireIssued(CertificateId id) {
        Certificate certificate = issued.get(id);
        if (certificate == null) {
            throw new ResourceNotFoundException("No certificate with id '" + id.value() + "'");
        }
        return certificate;
    }
}
