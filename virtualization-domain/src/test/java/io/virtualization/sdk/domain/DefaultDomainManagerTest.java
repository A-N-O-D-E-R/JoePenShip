package io.virtualization.sdk.domain;

import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.domain.support.FakeDnsProvider;
import io.virtualization.sdk.dns.DnsProviderRegistry;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsRecordSpec;
import io.virtualization.sdk.dns.DnsRecordType;
import io.virtualization.sdk.dns.DnsZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultDomainManagerTest {

    private InMemoryDomainRepository repository;
    private FakeDnsProvider cloudflare;
    private DomainManager manager;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDomainRepository();
        cloudflare = FakeDnsProvider.named("cloudflare")
                .withZone(new DnsZone("example.com", "cloudflare", "zone-1"));
        DnsProviderRegistry registry = new DnsProviderRegistry(Map.of("cloudflare", cloudflare));
        manager = new DefaultDomainManager(repository, registry);
    }

    @Nested
    class Registration {

        @Test
        void registerNormalizesAndPersistsWithNoProviderAssociated() {
            Domain domain = manager.register("EXAMPLE.com.");

            assertThat(domain.name()).isEqualTo("example.com");
            assertThat(domain.status()).isEqualTo(DomainStatus.ACTIVE);
            assertThat(domain.dnsProvider()).isNull();
            assertThat(manager.get(domain.id())).isEqualTo(domain);
        }

        @Test
        void findByNameNormalizesAndMatches() {
            Domain domain = manager.register("EXAMPLE.com.");

            assertThat(manager.findByName("example.com")).contains(domain);
        }

        @Test
        void findByNameUnknownIsEmpty() {
            assertThat(manager.findByName("unknown.com")).isEmpty();
        }

        @Test
        void listReturnsEveryRegisteredDomain() {
            Domain a = manager.register("example.com");
            Domain b = manager.register("example.net");

            assertThat(manager.list()).containsExactlyInAnyOrder(a, b);
        }
    }

    @Nested
    class ProviderAssociationAndZoneResolution {

        @Test
        void associateDnsProviderSetsItAndPersists() {
            Domain domain = manager.register("example.com");

            Domain associated = manager.associateDnsProvider(domain.id(), "cloudflare");

            assertThat(associated.dnsProvider()).isEqualTo("cloudflare");
            assertThat(manager.get(domain.id()).dnsProvider()).isEqualTo("cloudflare");
        }

        @Test
        void resolveZoneReturnsTheProvidersZone() {
            Domain domain = manager.register("example.com");
            manager.associateDnsProvider(domain.id(), "cloudflare");

            assertThat(manager.resolveZone(domain.id())).contains(new DnsZone("example.com", "cloudflare", "zone-1"));
        }

        @Test
        void resolveZoneWithNoKnownZoneOnProviderIsEmpty() {
            Domain domain = manager.register("unknown-zone.com");
            manager.associateDnsProvider(domain.id(), "cloudflare");

            assertThat(manager.resolveZone(domain.id())).isEmpty();
        }

        @Test
        void resolveZoneWalksUpFromASubdomainToItsOwningZone() {
            Domain domain = manager.register("app.example.com");
            manager.associateDnsProvider(domain.id(), "cloudflare");

            assertThat(manager.resolveZone(domain.id())).contains(new DnsZone("example.com", "cloudflare", "zone-1"));
        }

        @Test
        void resolveZoneWalksUpMultipleLabelsToItsOwningZone() {
            Domain domain = manager.register("api.staging.app.example.com");
            manager.associateDnsProvider(domain.id(), "cloudflare");

            assertThat(manager.resolveZone(domain.id())).contains(new DnsZone("example.com", "cloudflare", "zone-1"));
        }
    }

    @Nested
    class RecordManagement {

        private DomainId domainId;

        @BeforeEach
        void associate() {
            domainId = manager.register("example.com").id();
            manager.associateDnsProvider(domainId, "cloudflare");
        }

        @Test
        void createRecordDelegatesToTheAssociatedProvider() {
            DnsRecord record = manager.createRecord(domainId, new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.10", null, null));

            assertThat(record.name()).isEqualTo("app");
            assertThat(record.zone()).isEqualTo("example.com");
            assertThat(cloudflare.calls()).contains("createRecord:example.com:app");
        }

        @Test
        void updateRecordDelegatesToTheAssociatedProvider() {
            DnsRecord created = manager.createRecord(domainId, new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.10", null, null));

            DnsRecord updated = manager.updateRecord(
                    domainId, created.id(), new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.20", null, null));

            assertThat(updated.value()).isEqualTo("203.0.113.20");
            assertThat(cloudflare.calls()).contains("updateRecord:example.com:" + created.id());
        }

        @Test
        void deleteRecordDelegatesToTheAssociatedProvider() {
            DnsRecord created = manager.createRecord(domainId, new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.10", null, null));

            manager.deleteRecord(domainId, created.id());

            assertThat(cloudflare.calls()).contains("deleteRecord:example.com:" + created.id());
        }

        @Test
        void listRecordsReturnsEveryRecordInTheZone() {
            DnsRecord created = manager.createRecord(domainId, new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.10", null, null));

            assertThat(manager.listRecords(domainId)).containsExactly(created);
        }

        @Test
        void createRecordForASubdomainResolvesToTheParentZone() {
            DomainId subdomainId = manager.register("sub.example.com").id();
            manager.associateDnsProvider(subdomainId, "cloudflare");

            DnsRecord record = manager.createRecord(subdomainId, new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.10", null, null));

            assertThat(record.zone()).isEqualTo("example.com");
        }
    }

    @Nested
    class ErrorCases {

        @Test
        void getUnknownDomainThrowsResourceNotFound() {
            assertThatThrownBy(() -> manager.get(DomainId.generate())).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void resolveZoneWithNoAssociatedProviderThrowsIllegalState() {
            Domain domain = manager.register("example.com");

            assertThatThrownBy(() -> manager.resolveZone(domain.id())).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void associateUnknownDnsProviderThrowsConfigurationException() {
            Domain domain = manager.register("example.com");

            assertThatThrownBy(() -> manager.associateDnsProvider(domain.id(), "route53")).isInstanceOf(ConfigurationException.class);
        }

        @Test
        void createRecordWithUnknownZoneOnAssociatedProviderThrowsResourceNotFound() {
            Domain domain = manager.register("unknown-zone.com");
            manager.associateDnsProvider(domain.id(), "cloudflare");

            assertThatThrownBy(() -> manager.createRecord(domain.id(), new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.10", null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
