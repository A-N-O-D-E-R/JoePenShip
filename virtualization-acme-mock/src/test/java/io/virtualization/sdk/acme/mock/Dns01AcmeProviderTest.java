package io.virtualization.sdk.acme.mock;

import io.virtualization.sdk.acme.mock.support.FakeDnsProvider;
import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.certificate.CertificateRequest;
import io.virtualization.sdk.certificate.CertificateRequestOperation;
import io.virtualization.sdk.certificate.ChallengeType;
import io.virtualization.sdk.certificate.InMemoryCertificateStore;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.dns.DnsZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class Dns01AcmeProviderTest {

    private FakeDnsProvider dns;
    private InMemoryCertificateStore store;
    private Dns01AcmeProvider provider;

    @BeforeEach
    void setUp() {
        dns = FakeDnsProvider.named("fake-dns").withZone(new DnsZone("example.com", "fake-dns", "zone-1"));
        store = new InMemoryCertificateStore();
        provider = new Dns01AcmeProvider(dns, store);
    }

    @Test
    void apexDomainUsesChallengeRecordAtTheZoneApex() {
        CertificateRequestOperation operation = provider.request(
                CertificateRequest.builder().domains("example.com").issuer("mock").build());

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(dns.calls()).contains("createRecord:example.com:_acme-challenge");
    }

    @Test
    void subdomainUsesChallengeRecordWithTheLabelPrefix() {
        CertificateRequestOperation operation = provider.request(
                CertificateRequest.builder().domains("app.example.com").issuer("mock").build());

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(dns.calls()).contains("createRecord:example.com:_acme-challenge.app");
    }

    @Test
    void multiDomainRequestCreatesAndCleansUpOneRecordPerDomain() {
        CertificateRequestOperation operation = provider.request(
                CertificateRequest.builder().domains("example.com", "app.example.com", "api.example.com").issuer("mock").build());

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(dns.calls()).contains(
                "createRecord:example.com:_acme-challenge",
                "createRecord:example.com:_acme-challenge.app",
                "createRecord:example.com:_acme-challenge.api");
        long deletes = dns.calls().stream().filter(c -> c.startsWith("deleteRecord:")).count();
        assertThat(deletes).isEqualTo(3);
        assertThat(dns.records("example.com")).isEmpty();
    }

    @Test
    void laterDomainZoneLookupFailureStillCleansUpEarlierCreatedRecords() {
        CertificateRequestOperation operation = provider.request(
                CertificateRequest.builder().domains("example.com", "unknown-zone.net").issuer("mock").build());

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThat(dns.calls()).contains("createRecord:example.com:_acme-challenge", "deleteRecord:example.com:rec-1");
        assertThat(dns.records("example.com")).isEmpty();
    }

    @Test
    void oneRecordDeleteFailureDoesNotStopCleanupOfTheOthers() {
        dns.failDeleteFor("rec-1");

        CertificateRequestOperation operation = provider.request(
                CertificateRequest.builder().domains("example.com", "app.example.com").issuer("mock").build());

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(dns.calls()).contains("deleteRecord:example.com:rec-1", "deleteRecord:example.com:rec-2");
        // rec-1's deletion was simulated to fail, so it's still there; rec-2 was actually removed.
        assertThat(dns.records("example.com")).extracting(r -> r.id()).containsExactly("rec-1");
    }

    @Test
    void http01ChallengeIsRejected() {
        CertificateRequest request = CertificateRequest.builder().domains("example.com").issuer("mock").challenge(ChallengeType.HTTP_01).build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> provider.request(request))
                .isInstanceOf(io.virtualization.sdk.core.exception.UnsupportedCapabilityException.class);
    }

    @Test
    void successfulRequestLeavesMaterialLoadableInTheStore() {
        CertificateRequestOperation operation = provider.request(
                CertificateRequest.builder().domains("example.com").issuer("mock").build());
        operation.await(Duration.ofSeconds(5));

        Certificate certificate = operation.certificate().orElseThrow();
        assertThat(store.load(certificate.id())).isPresent();
    }
}
