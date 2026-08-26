package io.virtualization.sdk.spring;

import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.core.image.ImageProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderFactoryTest {

    @Test
    void missingTypeThrowsConfigurationException() {
        ProviderProperties properties = new ProviderProperties(null, null, null, null, true, null, null, null, null, null);

        assertThatThrownBy(() -> ProviderFactory.create("x", properties)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void unknownTypeThrowsConfigurationException() {
        ProviderProperties properties = new ProviderProperties("bogus", null, null, null, true, null, null, null, null, null);

        assertThatThrownBy(() -> ProviderFactory.create("x", properties))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("unknown type 'bogus'");
    }

    @Test
    void proxmoxBuildsSuccessfullyWithRequiredFields() {
        ProviderProperties properties = new ProviderProperties(
                "proxmox", "https://pve.example.com:8006", "root@pam!sdk", "secret", true, null, null, null, null, null);

        VirtualizationProvider provider = ProviderFactory.create("production", properties);

        assertThat(provider.type().id()).isEqualTo("proxmox");
    }

    @Test
    void proxmoxMissingTokenSecretThrowsWithExactMessage() {
        ProviderProperties properties = new ProviderProperties(
                "proxmox", "https://pve.example.com:8006", "root@pam!sdk", null, true, null, null, null, null, null);

        assertThatThrownBy(() -> ProviderFactory.create("production", properties))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Provider 'production' has type 'proxmox' but 'token-secret' is missing.");
    }

    @Test
    void incusMissingClientCertPathThrows() {
        ProviderProperties properties = new ProviderProperties(
                "incus", "https://incus.example.com:8443", null, null, true, null, "/some/key.pem", null, null, null);

        assertThatThrownBy(() -> ProviderFactory.create("containers", properties))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Provider 'containers' has type 'incus' but 'client-cert-path' is missing.");
    }

    @Test
    void incusUnreadableCertFileThrowsConfigurationException() {
        ProviderProperties properties = new ProviderProperties(
                "incus",
                "https://incus.example.com:8443",
                null,
                null,
                true,
                "/nonexistent/cert.pem",
                "/nonexistent/key.pem",
                null,
                null,
                null);

        assertThatThrownBy(() -> ProviderFactory.create("containers", properties)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void qemuMissingSocketAndHostThrows() {
        ProviderProperties properties = new ProviderProperties("qemu", null, null, null, true, null, null, null, null, null);

        assertThatThrownBy(() -> ProviderFactory.create("local", properties))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Provider 'local' has type 'qemu' but 'host' is missing.");
    }

    @Test
    void qemuHostWithoutPortThrows() {
        ProviderProperties properties =
                new ProviderProperties("qemu", null, null, null, true, null, null, null, "localhost", null);

        assertThatThrownBy(() -> ProviderFactory.create("local", properties))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Provider 'local' has type 'qemu' but 'port' is missing.");
    }

    @Test
    void createImageProviderBuildsOneForIncus() {
        ProviderProperties properties = new ProviderProperties(
                "incus", "https://incus.example.com:8443", null, null, true, certPath(), keyPath(), null, null, null);

        Optional<ImageProvider> images = ProviderFactory.createImageProvider("containers", properties);

        assertThat(images).isPresent();
        assertThat(images.get().name()).isEqualTo("incus");
    }

    @Test
    void createImageProviderIsEmptyForProxmoxAndQemu() {
        ProviderProperties proxmox = new ProviderProperties(
                "proxmox", "https://pve.example.com:8006", "root@pam!sdk", "secret", true, null, null, null, null, null);
        ProviderProperties qemu =
                new ProviderProperties("qemu", null, null, null, true, null, null, "/run/qemu.sock", null, null);

        assertThat(ProviderFactory.createImageProvider("production", proxmox)).isEmpty();
        assertThat(ProviderFactory.createImageProvider("local", qemu)).isEmpty();
    }

    private static String certPath() {
        return ProviderFactoryTest.class.getResource("/tls/client-cert.pem").getPath();
    }

    private static String keyPath() {
        return ProviderFactoryTest.class.getResource("/tls/client-key.pem").getPath();
    }
}
