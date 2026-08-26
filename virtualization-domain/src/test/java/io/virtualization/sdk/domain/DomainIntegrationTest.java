package io.virtualization.sdk.domain;

import io.virtualization.sdk.domain.support.FakeDnsProvider;
import io.virtualization.sdk.dns.DnsProviderRegistry;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsRecordSpec;
import io.virtualization.sdk.dns.DnsRecordType;
import io.virtualization.sdk.dns.DnsZone;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Wires {@link DefaultDomainManager} to a {@link FakeDnsProvider} end to end — mirrors {@code VpsIntegrationTest}. */
class DomainIntegrationTest {

    @Test
    void fullLifecycleThroughAResolvedZone() {
        FakeDnsProvider cloudflare = FakeDnsProvider.named("cloudflare")
                .withZone(new DnsZone("example.com", "cloudflare", "zone-1"));
        DomainManager manager = new DefaultDomainManager(
                new InMemoryDomainRepository(), new DnsProviderRegistry(Map.of("cloudflare", cloudflare)));

        Domain domain = manager.register("EXAMPLE.com.");
        assertThat(domain.name()).isEqualTo("example.com");

        manager.associateDnsProvider(domain.id(), "cloudflare");
        assertThat(manager.resolveZone(domain.id())).contains(new DnsZone("example.com", "cloudflare", "zone-1"));

        DnsRecord created = manager.createRecord(domain.id(), new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.10", 300L, null));
        assertThat(created.zone()).isEqualTo("example.com");

        DnsRecord updated = manager.updateRecord(
                domain.id(), created.id(), new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.20", 300L, null));
        assertThat(updated.value()).isEqualTo("203.0.113.20");

        manager.deleteRecord(domain.id(), created.id());
        assertThat(cloudflare.calls()).containsExactly(
                "createRecord:example.com:app", "updateRecord:example.com:" + created.id(), "deleteRecord:example.com:" + created.id());
    }
}
