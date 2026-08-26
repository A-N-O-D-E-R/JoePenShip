package io.virtualization.sdk.acme.mock;

import io.virtualization.sdk.acme.mock.support.FakeDnsProvider;
import io.virtualization.sdk.certificate.AcmeProviderRegistry;
import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.certificate.CertificateManager;
import io.virtualization.sdk.certificate.CertificateRequest;
import io.virtualization.sdk.certificate.CertificateStatus;
import io.virtualization.sdk.certificate.DefaultCertificateManager;
import io.virtualization.sdk.certificate.InMemoryCertificateRepository;
import io.virtualization.sdk.certificate.InMemoryCertificateStore;
import io.virtualization.sdk.dns.DnsZone;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wires {@link Dns01AcmeProvider} through {@link DefaultCertificateManager} end to end — mirrors
 * {@code CertificateIntegrationTest}/{@code DomainIntegrationTest} — and confirms the DNS-01
 * challenge record is gone afterward, proving cleanup ran through the full manager-mediated path,
 * not just the provider in isolation.
 */
class Dns01AcmeProviderIntegrationTest {

    @Test
    void fullLifecycleLeavesNoChallengeRecordBehind() {
        FakeDnsProvider dns = FakeDnsProvider.named("fake-dns").withZone(new DnsZone("example.com", "fake-dns", "zone-1"));
        Dns01AcmeProvider acmeProvider = new Dns01AcmeProvider(dns, new InMemoryCertificateStore());
        CertificateManager manager = new DefaultCertificateManager(
                new InMemoryCertificateRepository(), new AcmeProviderRegistry(Map.of("mock", acmeProvider)));

        Certificate requested = manager.requestCertificate(
                CertificateRequest.builder().domains("app.example.com").issuer("mock").build());
        assertThat(requested.status()).isEqualTo(CertificateStatus.ACTIVE);
        assertThat(manager.get(requested.id())).isEqualTo(requested);
        assertThat(dns.records("example.com")).isEmpty();

        Certificate renewed = manager.renew(requested.id());
        assertThat(renewed.id()).isEqualTo(requested.id());

        manager.revoke(requested.id());
        assertThat(manager.get(requested.id()).status()).isEqualTo(CertificateStatus.REVOKED);
    }
}
