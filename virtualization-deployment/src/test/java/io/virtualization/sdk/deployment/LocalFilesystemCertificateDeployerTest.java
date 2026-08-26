package io.virtualization.sdk.deployment;

import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.certificate.CertificateId;
import io.virtualization.sdk.certificate.CertificateMaterial;
import io.virtualization.sdk.certificate.CertificateStatus;
import io.virtualization.sdk.certificate.InMemoryCertificateStore;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFilesystemCertificateDeployerTest {

    private static Certificate sampleCertificate() {
        Instant now = Instant.now();
        return new Certificate(CertificateId.generate(), CertificateStatus.ACTIVE, List.of("example.com"), now, now.plusSeconds(3600), "letsencrypt");
    }

    private static CertificateMaterial sampleMaterial(String certBody) {
        return new CertificateMaterial(certBody, "key-pem", "chain-pem");
    }

    @Test
    void deployWritesAllThreeFilesIntoAFreshlyCreatedDirectory(@TempDir Path tempDir) throws IOException {
        InMemoryCertificateStore store = new InMemoryCertificateStore();
        Certificate certificate = sampleCertificate();
        store.store(certificate.id(), sampleMaterial("cert-v1"));
        LocalFilesystemCertificateDeployer deployer = new LocalFilesystemCertificateDeployer(store);
        Path certDir = tempDir.resolve("certs");

        deployer.deploy(certificate, new VpsDeploymentTarget("web-01", certDir, ReverseProxy.NGINX));

        assertThat(Files.readString(certDir.resolve("cert.pem"))).isEqualTo("cert-v1");
        assertThat(Files.readString(certDir.resolve("privkey.pem"))).isEqualTo("key-pem");
        assertThat(Files.readString(certDir.resolve("chain.pem"))).isEqualTo("chain-pem");
    }

    @Test
    void deployWithNoStoredMaterialThrowsResourceNotFound(@TempDir Path tempDir) {
        InMemoryCertificateStore store = new InMemoryCertificateStore();
        LocalFilesystemCertificateDeployer deployer = new LocalFilesystemCertificateDeployer(store);

        assertThatThrownBy(() -> deployer.deploy(sampleCertificate(), new VpsDeploymentTarget("web-01", tempDir, ReverseProxy.NONE)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deployWithUnsupportedTargetTypeThrowsUnsupportedCapability() {
        InMemoryCertificateStore store = new InMemoryCertificateStore();
        LocalFilesystemCertificateDeployer deployer = new LocalFilesystemCertificateDeployer(store);
        DeploymentTarget unsupportedTarget = new DeploymentTarget() {};

        assertThatThrownBy(() -> deployer.deploy(sampleCertificate(), unsupportedTarget))
                .isInstanceOf(UnsupportedCapabilityException.class);
    }

    @Test
    void redeployKeepsThePreviousVersionAsABackup(@TempDir Path tempDir) throws IOException {
        InMemoryCertificateStore store = new InMemoryCertificateStore();
        Certificate certificate = sampleCertificate();
        LocalFilesystemCertificateDeployer deployer = new LocalFilesystemCertificateDeployer(store);
        Path certDir = tempDir.resolve("certs");
        VpsDeploymentTarget target = new VpsDeploymentTarget("web-01", certDir, ReverseProxy.NGINX);

        store.store(certificate.id(), sampleMaterial("cert-v1"));
        deployer.deploy(certificate, target);

        store.store(certificate.id(), sampleMaterial("cert-v2"));
        deployer.deploy(certificate, target);

        assertThat(Files.readString(certDir.resolve("cert.pem"))).isEqualTo("cert-v2");
        assertThat(Files.readString(certDir.resolve("cert.pem.previous"))).isEqualTo("cert-v1");
    }

    @Test
    void reloadWithNoneDoesNotThrow(@TempDir Path tempDir) {
        InMemoryCertificateStore store = new InMemoryCertificateStore();
        Certificate certificate = sampleCertificate();
        store.store(certificate.id(), sampleMaterial("cert-v1"));
        LocalFilesystemCertificateDeployer deployer = new LocalFilesystemCertificateDeployer(store);

        deployer.deploy(certificate, new VpsDeploymentTarget("web-01", tempDir, ReverseProxy.NONE));
    }
}
