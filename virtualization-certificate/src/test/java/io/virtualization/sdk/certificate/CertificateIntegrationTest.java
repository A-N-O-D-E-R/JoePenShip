package io.virtualization.sdk.certificate;

import io.virtualization.sdk.certificate.support.FakeAcmeProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Wires {@link FakeAcmeProvider} through {@link DefaultCertificateManager} end to end — mirrors {@code DomainIntegrationTest}. */
class CertificateIntegrationTest {

    @Test
    void fullLifecycle() {
        CertificateManager manager = new DefaultCertificateManager(
                new InMemoryCertificateRepository(), new AcmeProviderRegistry(Map.of("letsencrypt", FakeAcmeProvider.succeeding())));

        Certificate requested = manager.requestCertificate(
                CertificateRequest.builder().domains("example.com", "www.example.com").issuer("letsencrypt").build());
        assertThat(requested.status()).isEqualTo(CertificateStatus.ACTIVE);
        assertThat(manager.get(requested.id())).isEqualTo(requested);

        Certificate renewed = manager.renew(requested.id());
        assertThat(renewed.id()).isEqualTo(requested.id());
        assertThat(renewed.expiresAt()).isAfterOrEqualTo(requested.expiresAt());

        manager.revoke(requested.id());
        assertThat(manager.get(requested.id()).status()).isEqualTo(CertificateStatus.REVOKED);
    }
}
