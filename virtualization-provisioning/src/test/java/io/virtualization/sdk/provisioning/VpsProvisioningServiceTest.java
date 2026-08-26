package io.virtualization.sdk.provisioning;

import io.virtualization.sdk.certificate.AcmeProviderRegistry;
import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.certificate.CertificateManager;
import io.virtualization.sdk.certificate.DefaultCertificateManager;
import io.virtualization.sdk.certificate.InMemoryCertificateRepository;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.domain.DefaultDomainManager;
import io.virtualization.sdk.domain.DomainManager;
import io.virtualization.sdk.domain.InMemoryDomainRepository;
import io.virtualization.sdk.dns.DnsProviderRegistry;
import io.virtualization.sdk.dns.DnsZone;
import io.virtualization.sdk.provisioning.support.FakeAcmeProvider;
import io.virtualization.sdk.provisioning.support.FakeDnsProvider;
import io.virtualization.sdk.provisioning.support.FakeVpsProvisioner;
import io.virtualization.sdk.vps.DefaultVpsManager;
import io.virtualization.sdk.vps.DnsProvisioningPolicy;
import io.virtualization.sdk.vps.InMemoryVpsRepository;
import io.virtualization.sdk.vps.NetworkConfiguration;
import io.virtualization.sdk.vps.VpsManager;
import io.virtualization.sdk.vps.VpsSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VpsProvisioningServiceTest {

    private static final ImageReference IMAGE = new ImageReference("incus", "images", "ubuntu/24.04");

    private VpsManager vpsManager;
    private DomainManager domainManager;
    private CertificateManager certificateManager;
    private FakeDnsProvider dns;
    private FakeAcmeProvider acme;
    private VpsProvisioningService service;

    @BeforeEach
    void setUp() {
        vpsManager = new DefaultVpsManager(new InMemoryVpsRepository(), FakeVpsProvisioner.succeeding());
        dns = FakeDnsProvider.named("cloudflare").withZone(new DnsZone("example.com", "cloudflare", "zone-1"));
        domainManager = new DefaultDomainManager(new InMemoryDomainRepository(), new DnsProviderRegistry(Map.of("cloudflare", dns)));
        acme = FakeAcmeProvider.succeeding();
        certificateManager = new DefaultCertificateManager(new InMemoryCertificateRepository(), new AcmeProviderRegistry(Map.of("letsencrypt", acme)));
        service = new VpsProvisioningService(vpsManager, domainManager, certificateManager);
    }

    private VpsSpec.Builder specBuilder() {
        return VpsSpec.builder("web-01", IMAGE);
    }

    @Test
    void basicProfileDoesNothingBeyondVpsCreation() {
        VpsSpec spec = specBuilder()
                .domains(java.util.List.of("app.example.com"))
                .dnsProvider("cloudflare").dnsPolicy(DnsProvisioningPolicy.CREATE)
                .tlsEnabled(true).tlsCertificateIssuer("letsencrypt")
                .network(new NetworkConfiguration("default", "203.0.113.10", null, "web-01"))
                .build();

        ProvisioningResult result = service.provision(spec, VpsProvisioningProfile.BASIC);

        assertThat(result.domains()).isEmpty();
        assertThat(result.certificate()).isNull();
        assertThat(dns.calls()).isEmpty();
        assertThat(acme.requestCallCount()).isZero();
    }

    @Test
    void domainProfileWithStaticIpCreatesARecordInTheWalkedZone() {
        VpsSpec spec = specBuilder()
                .domains(java.util.List.of("app.example.com"))
                .dnsProvider("cloudflare").dnsPolicy(DnsProvisioningPolicy.CREATE)
                .network(new NetworkConfiguration("default", "203.0.113.10", null, "web-01"))
                .build();

        ProvisioningResult result = service.provision(spec, VpsProvisioningProfile.DOMAIN);

        assertThat(result.domains()).hasSize(1);
        assertThat(result.domains().get(0).name()).isEqualTo("app.example.com");
        assertThat(dns.calls()).contains("createRecord:example.com:app");
    }

    @Test
    void domainProfileWithNoStaticIpSkipsDnsWithoutFailing() {
        VpsSpec spec = specBuilder()
                .domains(java.util.List.of("app.example.com"))
                .dnsProvider("cloudflare").dnsPolicy(DnsProvisioningPolicy.CREATE)
                .build();

        ProvisioningResult result = service.provision(spec, VpsProvisioningProfile.DOMAIN);

        assertThat(result.domains()).hasSize(1); // domain still registered/associated
        assertThat(dns.calls()).isEmpty(); // but no record created — nothing to point at
    }

    @Test
    void httpsProfileWithTlsEnabledRequestsACertificate() {
        VpsSpec spec = specBuilder()
                .domains(java.util.List.of("app.example.com"))
                .tlsEnabled(true).tlsCertificateIssuer("letsencrypt")
                .build();

        ProvisioningResult result = service.provision(spec, VpsProvisioningProfile.HTTPS);

        assertThat(result.certificate()).isNotNull();
        Certificate certificate = result.certificate();
        assertThat(certificate.domains()).containsExactly("app.example.com");
        assertThat(certificate.issuer()).isEqualTo("letsencrypt");
    }

    @Test
    void tlsEnabledWithNoIssuerThrowsIllegalState() {
        VpsSpec spec = specBuilder().domains(java.util.List.of("app.example.com")).tlsEnabled(true).build();

        assertThatThrownBy(() -> service.provision(spec, VpsProvisioningProfile.HTTPS)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void vpsCreationFailureShortCircuitsBeforeDnsOrCertificateCalls() {
        VpsManager failingVpsManager = new DefaultVpsManager(new InMemoryVpsRepository(), FakeVpsProvisioner.failing(new OperationException("boom")));
        VpsProvisioningService failingService = new VpsProvisioningService(failingVpsManager, domainManager, certificateManager);
        VpsSpec spec = specBuilder()
                .domains(java.util.List.of("app.example.com"))
                .dnsProvider("cloudflare").dnsPolicy(DnsProvisioningPolicy.CREATE)
                .tlsEnabled(true).tlsCertificateIssuer("letsencrypt")
                .network(new NetworkConfiguration("default", "203.0.113.10", null, "web-01"))
                .build();

        assertThatThrownBy(() -> failingService.provision(spec, VpsProvisioningProfile.HTTPS)).isInstanceOf(OperationException.class);

        assertThat(dns.calls()).isEmpty();
        assertThat(acme.requestCallCount()).isZero();
    }

    @Test
    void createAndUpdatePolicyUpdatesRatherThanDuplicatesOnASecondProvisionCall() {
        VpsSpec spec = specBuilder()
                .domains(java.util.List.of("app.example.com"))
                .dnsProvider("cloudflare").dnsPolicy(DnsProvisioningPolicy.CREATE_AND_UPDATE)
                .network(new NetworkConfiguration("default", "203.0.113.10", null, "web-01"))
                .build();

        service.provision(spec, VpsProvisioningProfile.DOMAIN);
        VpsSpec secondSpec = specBuilder()
                .domains(java.util.List.of("app.example.com"))
                .dnsProvider("cloudflare").dnsPolicy(DnsProvisioningPolicy.CREATE_AND_UPDATE)
                .network(new NetworkConfiguration("default", "203.0.113.20", null, "web-02"))
                .build();
        service.provision(secondSpec, VpsProvisioningProfile.DOMAIN);

        long createCalls = dns.calls().stream().filter(c -> c.startsWith("createRecord:")).count();
        long updateCalls = dns.calls().stream().filter(c -> c.startsWith("updateRecord:")).count();
        assertThat(createCalls).isEqualTo(1);
        assertThat(updateCalls).isEqualTo(1);
    }
}
