package io.virtualization.sdk.cli.config;

import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderFactoryTest {

    @Test
    void unknownTypeThrowsConfigurationException() {
        ProviderEntry entry = new ProviderEntry("bogus", null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> ProviderFactory.create("x", entry))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("unknown type 'bogus'");
    }

    @Test
    void missingTypeThrowsConfigurationException() {
        ProviderEntry entry = new ProviderEntry(null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> ProviderFactory.create("x", entry)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void proxmoxBuildsSuccessfullyWithRequiredFields() {
        ProviderEntry entry = new ProviderEntry(
                "proxmox", "https://pve.example.com:8006", "root@pam!sdk", "secret", null, null, null, null, null, null);

        VirtualizationProvider provider = ProviderFactory.create("production", entry);

        assertThat(provider.type().id()).isEqualTo("proxmox");
    }

    @Test
    void proxmoxMissingTokenSecretThrowsWithExactMessage() {
        ProviderEntry entry = new ProviderEntry(
                "proxmox", "https://pve.example.com:8006", "root@pam!sdk", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> ProviderFactory.create("production", entry))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Provider 'production' has type 'proxmox' but 'token-secret' is missing.");
    }

    @Test
    void incusMissingClientCertPathThrows() {
        ProviderEntry entry = new ProviderEntry(
                "incus", "https://incus.example.com:8443", null, null, null, null, "/some/key.pem", null, null, null);

        assertThatThrownBy(() -> ProviderFactory.create("containers", entry))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Provider 'containers' has type 'incus' but 'client-cert-path' is missing.");
    }

    @Test
    void incusUnreadableCertFileThrowsConfigurationException() {
        ProviderEntry entry = new ProviderEntry(
                "incus",
                "https://incus.example.com:8443",
                null,
                null,
                null,
                "/nonexistent/cert.pem",
                "/nonexistent/key.pem",
                null,
                null,
                null);

        assertThatThrownBy(() -> ProviderFactory.create("containers", entry)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void qemuMissingSocketAndHostThrows() {
        ProviderEntry entry = new ProviderEntry("qemu", null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> ProviderFactory.create("local", entry))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Provider 'local' has type 'qemu' but 'host' is missing.");
    }

    @Test
    void qemuHostWithoutPortThrows() {
        ProviderEntry entry = new ProviderEntry("qemu", null, null, null, null, null, null, null, "localhost", null);

        assertThatThrownBy(() -> ProviderFactory.create("local", entry))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Provider 'local' has type 'qemu' but 'port' is missing.");
    }
}
