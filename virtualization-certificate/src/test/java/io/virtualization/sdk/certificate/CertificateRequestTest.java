package io.virtualization.sdk.certificate;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CertificateRequestTest {

    @Test
    void buildsWithDomainsIssuerAndDefaultChallenge() {
        CertificateRequest request = CertificateRequest.builder().domains("example.com", "www.example.com").issuer("letsencrypt").build();

        assertThat(request.domains()).containsExactly("example.com", "www.example.com");
        assertThat(request.issuer()).isEqualTo("letsencrypt");
        assertThat(request.challenge()).isEqualTo(ChallengeType.DNS_01);
    }

    @Test
    void challengeCanBeOverridden() {
        CertificateRequest request = CertificateRequest.builder()
                .domains("example.com").issuer("letsencrypt").challenge(ChallengeType.HTTP_01).build();

        assertThat(request.challenge()).isEqualTo(ChallengeType.HTTP_01);
    }

    @Test
    void listOverloadWorksTheSameAsVarargs() {
        CertificateRequest request = CertificateRequest.builder().domains(List.of("example.com")).issuer("letsencrypt").build();

        assertThat(request.domains()).containsExactly("example.com");
    }

    @Test
    void rejectsEmptyDomains() {
        assertThatIllegalArgumentException().isThrownBy(() -> CertificateRequest.builder().issuer("letsencrypt").build());
    }

    @Test
    void rejectsBlankDomainEntry() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> CertificateRequest.builder().domains("example.com", " ").issuer("letsencrypt").build());
    }

    @Test
    void rejectsNullIssuer() {
        assertThatNullPointerException().isThrownBy(() -> CertificateRequest.builder().domains("example.com").build());
    }

    @Test
    void rejectsBlankIssuer() {
        assertThatIllegalArgumentException().isThrownBy(() -> CertificateRequest.builder().domains("example.com").issuer(" ").build());
    }
}
