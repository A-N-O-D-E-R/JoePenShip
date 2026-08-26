package io.virtualization.sdk.certificate;

import io.virtualization.sdk.certificate.support.FakeAcmeProvider;
import io.virtualization.sdk.core.exception.OperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CertificateRenewalSchedulerTest {

    private InMemoryCertificateRepository repository;
    private FakeAcmeProvider provider;
    private CertificateManager manager;
    private CertificateRenewalScheduler scheduler;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCertificateRepository();
        provider = FakeAcmeProvider.succeeding();
        manager = new DefaultCertificateManager(repository, new AcmeProviderRegistry(Map.of("letsencrypt", provider)));
        scheduler = new CertificateRenewalScheduler(manager);
    }

    private Certificate seed(CertificateStatus status, Instant expiresAt) {
        Certificate certificate = new Certificate(
                CertificateId.generate(), status, List.of("example.com"), expiresAt != null ? expiresAt.minus(Duration.ofDays(60)) : null,
                expiresAt, "letsencrypt");
        repository.save(certificate);
        provider.seed(certificate);
        return certificate;
    }

    @Test
    void certificateExpiringWithinTheWindowIsRenewed() {
        Certificate dueSoon = seed(CertificateStatus.ACTIVE, Instant.now().plus(Duration.ofDays(10)));

        List<CertificateRenewalResult> results = scheduler.renewDue(Duration.ofDays(30));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).certificateId()).isEqualTo(dueSoon.id());
        assertThat(results.get(0).renewed()).isTrue();
        assertThat(manager.get(dueSoon.id()).expiresAt()).isAfter(dueSoon.expiresAt());
    }

    @Test
    void certificateNotYetDueIsSkipped() {
        seed(CertificateStatus.ACTIVE, Instant.now().plus(Duration.ofDays(60)));

        List<CertificateRenewalResult> results = scheduler.renewDue(Duration.ofDays(30));

        assertThat(results).isEmpty();
    }

    @Test
    void certificateWithNoExpiresAtIsSkipped() {
        seed(CertificateStatus.REQUESTED, null);

        List<CertificateRenewalResult> results = scheduler.renewDue(Duration.ofDays(30));

        assertThat(results).isEmpty();
    }

    @Test
    void revokedCertificateIsNeverAttempted() {
        Certificate revoked = seed(CertificateStatus.REVOKED, Instant.now().plus(Duration.ofDays(1)));

        List<CertificateRenewalResult> results = scheduler.renewDue(Duration.ofDays(30));

        assertThat(results).isEmpty();
        assertThat(manager.get(revoked.id()).status()).isEqualTo(CertificateStatus.REVOKED);
    }

    @Test
    void failedCertificateIsNeverAttempted() {
        seed(CertificateStatus.FAILED, Instant.now().plus(Duration.ofDays(1)));

        List<CertificateRenewalResult> results = scheduler.renewDue(Duration.ofDays(30));

        assertThat(results).isEmpty();
    }

    @Test
    void renewalFailureIsReportedAndLeavesTheExistingCertificateUntouched() {
        Certificate dueSoon = seed(CertificateStatus.ACTIVE, Instant.now().plus(Duration.ofDays(10)));
        provider.renewFails(new OperationException("CA unreachable"));

        List<CertificateRenewalResult> results = scheduler.renewDue(Duration.ofDays(30));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).renewed()).isFalse();
        assertThat(results.get(0).error()).contains("CA unreachable");
        // the old, still-valid certificate is untouched — not replaced with anything broken.
        assertThat(manager.get(dueSoon.id())).isEqualTo(dueSoon);
    }

    @Test
    void oneFailureDoesNotStopOthersInTheSameRun() {
        Certificate willFail = seed(CertificateStatus.ACTIVE, Instant.now().plus(Duration.ofDays(5)));
        provider.renewFails(new OperationException("CA unreachable"));
        // a second provider that succeeds, for a second certificate under a different issuer.
        FakeAcmeProvider provider2 = FakeAcmeProvider.succeeding();
        CertificateManager manager2 = new DefaultCertificateManager(
                repository, new AcmeProviderRegistry(Map.of("letsencrypt", provider, "zerossl", provider2)));
        Certificate willSucceed = new Certificate(
                CertificateId.generate(), CertificateStatus.ACTIVE, List.of("example.net"), Instant.now().minus(Duration.ofDays(60)),
                Instant.now().plus(Duration.ofDays(5)), "zerossl");
        repository.save(willSucceed);
        provider2.seed(willSucceed);

        List<CertificateRenewalResult> results = new CertificateRenewalScheduler(manager2).renewDue(Duration.ofDays(30));

        assertThat(results).hasSize(2);
        assertThat(results).anySatisfy(r -> {
            assertThat(r.certificateId()).isEqualTo(willFail.id());
            assertThat(r.renewed()).isFalse();
        });
        assertThat(results).anySatisfy(r -> {
            assertThat(r.certificateId()).isEqualTo(willSucceed.id());
            assertThat(r.renewed()).isTrue();
        });
    }

    @Test
    void defaultWindowIsThirtyDays() {
        assertThat(CertificateRenewalScheduler.DEFAULT_RENEW_BEFORE).isEqualTo(Duration.ofDays(30));
    }
}
