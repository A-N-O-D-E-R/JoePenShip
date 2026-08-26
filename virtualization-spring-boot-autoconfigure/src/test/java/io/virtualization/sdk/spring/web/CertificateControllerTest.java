package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.certificate.AcmeProviderRegistry;
import io.virtualization.sdk.certificate.CertificateManager;
import io.virtualization.sdk.certificate.CertificateStatus;
import io.virtualization.sdk.certificate.DefaultCertificateManager;
import io.virtualization.sdk.certificate.InMemoryCertificateRepository;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.spring.web.support.FakeAcmeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertificateControllerTest {

    private CertificateController controller;

    @BeforeEach
    void setUp() {
        CertificateManager manager = new DefaultCertificateManager(
                new InMemoryCertificateRepository(), new AcmeProviderRegistry(Map.of("letsencrypt", new FakeAcmeProvider())));
        controller = new CertificateController(manager);
    }

    private CreateCertificateRequestBody sampleRequest() {
        return new CreateCertificateRequestBody(List.of("example.com"), "letsencrypt", null);
    }

    @Test
    void createReturnsTheActiveCertificate() {
        CertificateView created = controller.create(sampleRequest());

        assertThat(created.status()).isEqualTo(CertificateStatus.ACTIVE);
        assertThat(created.domains()).containsExactly("example.com");
    }

    @Test
    void getReturnsTheCreatedCertificate() {
        CertificateView created = controller.create(sampleRequest());

        assertThat(controller.get(created.id())).isEqualTo(created);
    }

    @Test
    void getUnknownCertificateThrowsResourceNotFound() {
        assertThatThrownBy(() -> controller.get("cert-does-not-exist")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listReturnsEveryRequestedCertificate() {
        CertificateView created = controller.create(sampleRequest());

        assertThat(controller.list()).containsExactly(created);
    }

    @Test
    void renewReturnsAnUpdatedCertificate() {
        CertificateView created = controller.create(sampleRequest());

        CertificateView renewed = controller.renew(created.id());

        assertThat(renewed.id()).isEqualTo(created.id());
        assertThat(renewed.expiresAt()).isAfterOrEqualTo(created.expiresAt());
    }

    @Test
    void revokeMarksTheCertificateRevoked() {
        CertificateView created = controller.create(sampleRequest());

        controller.revoke(created.id());

        assertThat(controller.get(created.id()).status()).isEqualTo(CertificateStatus.REVOKED);
    }
}
