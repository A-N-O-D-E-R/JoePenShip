package io.virtualization.sdk.cli.config;

import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigLoaderTest {

    @Test
    void substituteEnvVarsReplacesKnownVariable() {
        String path = System.getenv("PATH");
        assertThat(path).isNotNull();

        assertThat(ConfigLoader.substituteEnvVars("value: ${PATH}")).isEqualTo("value: " + path);
    }

    @Test
    void substituteEnvVarsThrowsForUnknownVariable() {
        assertThatThrownBy(() -> ConfigLoader.substituteEnvVars("value: ${THIS_VAR_DOES_NOT_EXIST_12345}"))
                .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void loadClientFromYamlBuildsConfiguredProxmoxProvider() {
        String yaml =
                """
                virtualization:
                  providers:
                    production:
                      type: proxmox
                      endpoint: https://pve.example.com:8006
                      token-id: root@pam!sdk
                      token-secret: dummy-secret
                """;

        VirtualizationClient client = ConfigLoader.loadClientFromYaml(yaml);

        assertThat(client.providers()).containsOnlyKeys("production");
        assertThat(client.provider("production").type().id()).isEqualTo("proxmox");
    }

    @Test
    void loadClientFromYamlMissingRequiredFieldThrowsWithExactMessage() {
        String yaml =
                """
                virtualization:
                  providers:
                    production:
                      type: proxmox
                      endpoint: https://pve.example.com:8006
                      token-id: root@pam!sdk
                """;

        assertThatThrownBy(() -> ConfigLoader.loadClientFromYaml(yaml))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Provider 'production' has type 'proxmox' but 'token-secret' is missing.");
    }

    @Test
    void loadClientFromYamlSubstitutesEnvVarsInValues() {
        String yaml =
                """
                virtualization:
                  providers:
                    production:
                      type: proxmox
                      endpoint: https://pve.example.com:8006
                      token-id: root@pam!sdk
                      token-secret: ${PATH}
                """;

        VirtualizationClient client = ConfigLoader.loadClientFromYaml(yaml);

        assertThat(client.providers()).containsOnlyKeys("production");
    }

    @Test
    void loadClientThrowsWhenFileMissing() {
        Path missing = Path.of("/nonexistent/path/config.yaml");

        assertThatThrownBy(() -> ConfigLoader.loadClient(missing)).isInstanceOf(ConfigurationException.class);
    }
}
