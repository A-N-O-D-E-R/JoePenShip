package io.virtualization.sdk.certificate;

import io.virtualization.sdk.core.exception.VirtualizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Renews every {@link CertificateManager}-known certificate due within a configurable window of
 * expiring (spec §27, default {@link #DEFAULT_RENEW_BEFORE} = 30 days). {@code REVOKED}/{@code
 * FAILED} certificates are never attempted — they need explicit recovery, not a blind retry
 * (matches {@link DefaultCertificateManager#renew}'s own guard).
 *
 * <p>This class does not schedule itself — {@link #renewDue} is a single, synchronous, on-demand
 * batch run; something else (a Spring {@code @Scheduled} method, a CLI cron job) decides when to
 * call it, per {@code virtualization.certificates.renewal.check-interval}. One certificate's
 * renewal failure never stops the others in the same run — spec §44: a failed renewal must leave
 * the certificate's current, still-valid state alone, not replace it with anything broken; this
 * class simply never overwrites a row it didn't successfully renew.
 */
public final class CertificateRenewalScheduler {

    public static final Duration DEFAULT_RENEW_BEFORE = Duration.ofDays(30);

    private static final Logger log = LoggerFactory.getLogger(CertificateRenewalScheduler.class);

    private final CertificateManager manager;

    public CertificateRenewalScheduler(CertificateManager manager) {
        this.manager = Objects.requireNonNull(manager, "manager must not be null");
    }

    public List<CertificateRenewalResult> renewDue() {
        return renewDue(DEFAULT_RENEW_BEFORE);
    }

    public List<CertificateRenewalResult> renewDue(Duration renewBefore) {
        Objects.requireNonNull(renewBefore, "renewBefore must not be null");
        Instant threshold = Instant.now().plus(renewBefore);
        List<CertificateRenewalResult> results = new ArrayList<>();
        for (Certificate certificate : manager.list()) {
            if (isDue(certificate, threshold)) {
                results.add(attemptRenew(certificate.id()));
            }
        }
        return results;
    }

    private static boolean isDue(Certificate certificate, Instant threshold) {
        if (certificate.status() == CertificateStatus.REVOKED || certificate.status() == CertificateStatus.FAILED) {
            return false;
        }
        return certificate.expiresAt() != null && !certificate.expiresAt().isAfter(threshold);
    }

    private CertificateRenewalResult attemptRenew(CertificateId id) {
        try {
            manager.renew(id);
            return CertificateRenewalResult.renewed(id);
        } catch (VirtualizationException e) {
            log.warn("Certificate renewal failed for '{}': {}", id.value(), e.getMessage());
            return CertificateRenewalResult.failed(id, e.getMessage());
        }
    }
}
