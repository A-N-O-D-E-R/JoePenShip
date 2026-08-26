package io.virtualization.sdk.certificate;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CertificateTest {

    private static final CertificateId ID = CertificateId.generate();

    @Test
    void rejectsNullId() {
        assertThatNullPointerException().isThrownBy(
                () -> new Certificate(null, CertificateStatus.REQUESTED, List.of("example.com"), null, null, "letsencrypt"));
    }

    @Test
    void rejectsNullStatus() {
        assertThatNullPointerException().isThrownBy(
                () -> new Certificate(ID, null, List.of("example.com"), null, null, "letsencrypt"));
    }

    @Test
    void rejectsNullDomains() {
        assertThatNullPointerException().isThrownBy(
                () -> new Certificate(ID, CertificateStatus.REQUESTED, null, null, null, "letsencrypt"));
    }

    @Test
    void rejectsEmptyDomains() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new Certificate(ID, CertificateStatus.REQUESTED, List.of(), null, null, "letsencrypt"));
    }

    @Test
    void rejectsBlankDomainEntry() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new Certificate(ID, CertificateStatus.REQUESTED, List.of("example.com", " "), null, null, "letsencrypt"));
    }

    @Test
    void rejectsNullIssuer() {
        assertThatNullPointerException().isThrownBy(
                () -> new Certificate(ID, CertificateStatus.REQUESTED, List.of("example.com"), null, null, null));
    }

    @Test
    void rejectsBlankIssuer() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new Certificate(ID, CertificateStatus.REQUESTED, List.of("example.com"), null, null, " "));
    }

    @Test
    void allowsNullIssuedAtAndExpiresAt() {
        Certificate certificate = new Certificate(ID, CertificateStatus.REQUESTED, List.of("example.com"), null, null, "letsencrypt");

        assertThat(certificate.issuedAt()).isNull();
        assertThat(certificate.expiresAt()).isNull();
    }

    @Test
    void acceptsIssuedAtAndExpiresAtWhenActive() {
        Instant now = Instant.now();
        Certificate certificate = new Certificate(ID, CertificateStatus.ACTIVE, List.of("example.com"), now, now.plusSeconds(60), "letsencrypt");

        assertThat(certificate.issuedAt()).isEqualTo(now);
        assertThat(certificate.expiresAt()).isEqualTo(now.plusSeconds(60));
    }
}
