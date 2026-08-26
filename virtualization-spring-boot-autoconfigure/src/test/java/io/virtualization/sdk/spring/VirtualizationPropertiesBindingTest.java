package io.virtualization.sdk.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualizationPropertiesBindingTest {

    @Configuration
    @EnableConfigurationProperties(VirtualizationProperties.class)
    static class TestConfig {}

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Test
    void bindsProviderFieldsFromKebabCaseYamlKeys() {
        contextRunner
                .withPropertyValues(
                        "virtualization.providers.production.type=proxmox",
                        "virtualization.providers.production.endpoint=https://pve.example.com:8006",
                        "virtualization.providers.production.token-id=root@pam!sdk",
                        "virtualization.providers.production.token-secret=s3cr3t")
                .run(context -> {
                    VirtualizationProperties properties = context.getBean(VirtualizationProperties.class);
                    ProviderProperties production = properties.providers().get("production");

                    assertThat(production.type()).isEqualTo("proxmox");
                    assertThat(production.endpoint()).isEqualTo("https://pve.example.com:8006");
                    assertThat(production.tokenId()).isEqualTo("root@pam!sdk");
                    assertThat(production.tokenSecret()).isEqualTo("s3cr3t");
                });
    }

    @Test
    void verifySslDefaultsToTrueWhenOmitted() {
        contextRunner
                .withPropertyValues(
                        "virtualization.providers.production.type=proxmox",
                        "virtualization.providers.production.endpoint=https://pve.example.com:8006")
                .run(context -> {
                    ProviderProperties production =
                            context.getBean(VirtualizationProperties.class).providers().get("production");
                    assertThat(production.verifySsl()).isTrue();
                });
    }

    @Test
    void verifySslCanBeDisabled() {
        contextRunner
                .withPropertyValues(
                        "virtualization.providers.production.type=proxmox", "virtualization.providers.production.verify-ssl=false")
                .run(context -> {
                    ProviderProperties production =
                            context.getBean(VirtualizationProperties.class).providers().get("production");
                    assertThat(production.verifySsl()).isFalse();
                });
    }

    @Test
    void multipleProvidersBindIndependently() {
        contextRunner
                .withPropertyValues(
                        "virtualization.providers.production.type=proxmox",
                        "virtualization.providers.containers.type=incus",
                        "virtualization.providers.local.type=qemu")
                .run(context -> {
                    var providers = context.getBean(VirtualizationProperties.class).providers();
                    assertThat(providers).containsOnlyKeys("production", "containers", "local");
                });
    }

    @Test
    void emptyConfigurationBindsToEmptyMap() {
        contextRunner.run(context -> {
            VirtualizationProperties properties = context.getBean(VirtualizationProperties.class);
            assertThat(properties.providers()).isEmpty();
        });
    }
}
