package io.virtualization.sdk.dns.mock;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsRecordSpec;
import io.virtualization.sdk.dns.DnsRecordType;
import io.virtualization.sdk.dns.DnsZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryDnsProviderTest {

    private InMemoryDnsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new InMemoryDnsProvider("mock");
    }

    @Test
    void nameReturnsConstructorValue() {
        assertThat(provider.name()).isEqualTo("mock");
    }

    @Nested
    class Zones {

        @Test
        void addZoneRegistersItAndItAppearsInZonesAndGetZone() {
            DnsZone zone = provider.addZone("example.com");

            assertThat(zone.name()).isEqualTo("example.com");
            assertThat(zone.provider()).isEqualTo("mock");
            assertThat(provider.zones()).containsExactly(zone);
            assertThat(provider.getZone("example.com")).contains(zone);
        }

        @Test
        void getZoneUnknownIsEmpty() {
            assertThat(provider.getZone("unknown.com")).isEmpty();
        }

        @Test
        void multipleZonesAreIndependent() {
            provider.addZone("example.com");
            provider.addZone("example.net");

            assertThat(provider.zones()).extracting(DnsZone::name).containsExactlyInAnyOrder("example.com", "example.net");
        }
    }

    @Nested
    class Records {

        @BeforeEach
        void addZone() {
            provider.addZone("example.com");
        }

        @Test
        void createRecordAddsItToTheZone() {
            DnsRecord record = provider.createRecord("example.com", new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.10", 300L, null));

            assertThat(record.id()).isNotBlank();
            assertThat(record.zone()).isEqualTo("example.com");
            assertThat(provider.records("example.com")).containsExactly(record);
        }

        @Test
        void createRecordOnUnknownZoneThrowsResourceNotFound() {
            assertThatThrownBy(() -> provider.createRecord("unknown.com", new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.10", null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void updateRecordReplacesItInPlace() {
            DnsRecord created = provider.createRecord("example.com", new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.10", null, null));

            DnsRecord updated = provider.updateRecord(
                    "example.com", created.id(), new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.20", null, null));

            assertThat(updated.id()).isEqualTo(created.id());
            assertThat(updated.value()).isEqualTo("203.0.113.20");
            assertThat(provider.records("example.com")).containsExactly(updated);
        }

        @Test
        void updateUnknownRecordThrowsResourceNotFound() {
            assertThatThrownBy(() -> provider.updateRecord(
                    "example.com", "rec-999", new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.10", null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void deleteRecordRemovesIt() {
            DnsRecord created = provider.createRecord("example.com", new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.10", null, null));

            provider.deleteRecord("example.com", created.id());

            assertThat(provider.records("example.com")).isEmpty();
        }

        @Test
        void deleteUnknownRecordThrowsResourceNotFound() {
            assertThatThrownBy(() -> provider.deleteRecord("example.com", "rec-999")).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void recordsOnUnknownZoneThrowsResourceNotFound() {
            assertThatThrownBy(() -> provider.records("unknown.com")).isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
