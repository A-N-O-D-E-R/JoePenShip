package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.domain.DefaultDomainManager;
import io.virtualization.sdk.domain.DomainManager;
import io.virtualization.sdk.domain.InMemoryDomainRepository;
import io.virtualization.sdk.dns.DnsProviderRegistry;
import io.virtualization.sdk.dns.DnsRecordType;
import io.virtualization.sdk.dns.DnsZone;
import io.virtualization.sdk.spring.web.support.FakeDnsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainControllerTest {

    private DomainManager domainManager;
    private DomainController controller;

    @BeforeEach
    void setUp() {
        FakeDnsProvider dns = FakeDnsProvider.named("cloudflare").withZone(new DnsZone("example.com", "cloudflare", "zone-1"));
        domainManager = new DefaultDomainManager(new InMemoryDomainRepository(), new DnsProviderRegistry(Map.of("cloudflare", dns)));
        controller = new DomainController(domainManager);
    }

    @Test
    void listReturnsEveryRegisteredDomain() {
        domainManager.register("example.com");

        assertThat(controller.list()).extracting(DomainView::name).containsExactly("example.com");
    }

    @Test
    void getReturnsTheNormalizedDomain() {
        domainManager.register("EXAMPLE.com.");

        assertThat(controller.get("example.com").name()).isEqualTo("example.com");
    }

    @Test
    void getUnknownDomainThrowsResourceNotFound() {
        assertThatThrownBy(() -> controller.get("unknown.com")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createAndListRecords() {
        var domain = domainManager.register("example.com");
        domainManager.associateDnsProvider(domain.id(), "cloudflare");

        DnsRecordView created = controller.createRecord(
                "example.com", new CreateDnsRecordRequestBody("app", DnsRecordType.A, "203.0.113.10", null, null));

        assertThat(created.name()).isEqualTo("app");
        assertThat(controller.listRecords("example.com")).containsExactly(created);
    }

    @Test
    void deleteRecordRemovesIt() {
        var domain = domainManager.register("example.com");
        domainManager.associateDnsProvider(domain.id(), "cloudflare");
        DnsRecordView created = controller.createRecord(
                "example.com", new CreateDnsRecordRequestBody("app", DnsRecordType.A, "203.0.113.10", null, null));

        controller.deleteRecord("example.com", created.id());

        assertThat(controller.listRecords("example.com")).isEmpty();
    }
}
