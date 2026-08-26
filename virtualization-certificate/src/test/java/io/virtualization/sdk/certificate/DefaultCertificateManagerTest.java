package io.virtualization.sdk.certificate;

import io.virtualization.sdk.certificate.support.FakeAcmeProvider;
import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultCertificateManagerTest {

    private InMemoryCertificateRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCertificateRepository();
    }

    private CertificateManager manager(AcmeProvider provider) {
        return new DefaultCertificateManager(repository, new AcmeProviderRegistry(Map.of("letsencrypt", provider)));
    }

    private static CertificateRequest sampleRequest() {
        return CertificateRequest.builder().domains("example.com").issuer("letsencrypt").build();
    }

    @Nested
    class Request {

        @Test
        void successPersistsAndReturnsTheCertificate() {
            CertificateManager manager = manager(FakeAcmeProvider.succeeding());

            Certificate certificate = manager.requestCertificate(sampleRequest());

            assertThat(certificate.status()).isEqualTo(CertificateStatus.ACTIVE);
            assertThat(certificate.domains()).containsExactly("example.com");
            assertThat(manager.get(certificate.id())).isEqualTo(certificate);
        }

        @Test
        void unknownIssuerThrowsConfigurationException() {
            CertificateManager manager = manager(FakeAcmeProvider.succeeding());
            CertificateRequest request = CertificateRequest.builder().domains("example.com").issuer("zerossl").build();

            assertThatThrownBy(() -> manager.requestCertificate(request)).isInstanceOf(ConfigurationException.class);
        }

        @Test
        void acmeFailurePropagatesAndPersistsNothing() {
            CertificateManager manager = manager(FakeAcmeProvider.failing(new OperationException("acme rejected")));

            assertThatThrownBy(() -> manager.requestCertificate(sampleRequest())).isInstanceOf(OperationException.class);
            assertThat(manager.list()).isEmpty();
        }
    }

    @Nested
    class Renew {

        @Test
        void successUpdatesTheRepositoryRow() {
            CertificateManager manager = manager(FakeAcmeProvider.succeeding());
            Certificate original = manager.requestCertificate(sampleRequest());

            Certificate renewed = manager.renew(original.id());

            assertThat(renewed.id()).isEqualTo(original.id());
            assertThat(renewed.status()).isEqualTo(CertificateStatus.ACTIVE);
            assertThat(manager.get(original.id())).isEqualTo(renewed);
        }

        @Test
        void revokedCertificateCannotBeRenewed() {
            CertificateManager manager = manager(FakeAcmeProvider.succeeding());
            Certificate certificate = manager.requestCertificate(sampleRequest());
            manager.revoke(certificate.id());

            assertThatThrownBy(() -> manager.renew(certificate.id())).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void unknownIdThrowsResourceNotFound() {
            CertificateManager manager = manager(FakeAcmeProvider.succeeding());

            assertThatThrownBy(() -> manager.renew(CertificateId.generate())).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class Revoke {

        @Test
        void delegatesToTheProviderAndPersistsRevokedStatus() {
            CertificateManager manager = manager(FakeAcmeProvider.succeeding());
            Certificate certificate = manager.requestCertificate(sampleRequest());

            manager.revoke(certificate.id());

            assertThat(manager.get(certificate.id()).status()).isEqualTo(CertificateStatus.REVOKED);
        }
    }

    @Nested
    class Inspection {

        @Test
        void getUnknownCertificateThrowsResourceNotFound() {
            CertificateManager manager = manager(FakeAcmeProvider.succeeding());

            assertThatThrownBy(() -> manager.get(CertificateId.generate())).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void listReturnsEveryRequestedCertificate() {
            CertificateManager manager = manager(FakeAcmeProvider.succeeding());
            Certificate a = manager.requestCertificate(sampleRequest());
            Certificate b = manager.requestCertificate(CertificateRequest.builder().domains("example.net").issuer("letsencrypt").build());

            assertThat(manager.list()).containsExactlyInAnyOrder(a, b);
        }
    }
}
