package io.virtualization.sdk.spring;

import io.virtualization.sdk.core.ProviderRegistry;
import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.image.ImageProviderRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualizationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(VirtualizationAutoConfiguration.class));

    @Test
    void contextStartsWithNoProvidersConfigured() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ProviderRegistry.class);
            assertThat(context).hasSingleBean(ImageProviderRegistry.class);
            assertThat(context).hasSingleBean(VirtualizationClient.class);
            assertThat(context.getBean(ProviderRegistry.class).all()).isEmpty();
            assertThat(context.getBean(ImageProviderRegistry.class).all()).isEmpty();
        });
    }

    @Test
    void wiresImageProviderForIncusProviderOnly() {
        contextRunner
                .withPropertyValues(
                        "virtualization.providers.production.type=proxmox",
                        "virtualization.providers.production.endpoint=https://pve.example.com:8006",
                        "virtualization.providers.production.token-id=root@pam!sdk",
                        "virtualization.providers.production.token-secret=s3cr3t",
                        "virtualization.providers.containers.type=incus",
                        "virtualization.providers.containers.endpoint=https://incus.example.com:8443",
                        "virtualization.providers.containers.client-cert-path=" + certPath(),
                        "virtualization.providers.containers.client-key-path=" + keyPath())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ImageProviderRegistry registry = context.getBean(ImageProviderRegistry.class);
                    assertThat(registry.all()).containsOnlyKeys("containers");

                    VirtualizationClient client = context.getBean(VirtualizationClient.class);
                    assertThat(client.images("containers").name()).isEqualTo("incus");
                });
    }

    @Test
    void userDefinedImageProviderRegistryBeanIsNotOverridden() {
        contextRunner.withUserConfiguration(CustomImageRegistryConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ImageProviderRegistry.class);
            assertThat(context.getBean(ImageProviderRegistry.class)).isSameAs(CustomImageRegistryConfig.CUSTOM_REGISTRY);
        });
    }

    @Test
    void wiresOneConfiguredProviderIntoTheRegistry() {
        contextRunner
                .withPropertyValues(
                        "virtualization.providers.production.type=proxmox",
                        "virtualization.providers.production.endpoint=https://pve.example.com:8006",
                        "virtualization.providers.production.token-id=root@pam!sdk",
                        "virtualization.providers.production.token-secret=s3cr3t")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ProviderRegistry registry = context.getBean(ProviderRegistry.class);
                    assertThat(registry.all()).containsOnlyKeys("production");
                    assertThat(registry.get("production").type().id()).isEqualTo("proxmox");

                    VirtualizationClient client = context.getBean(VirtualizationClient.class);
                    assertThat(client.provider("production").type().id()).isEqualTo("proxmox");
                });
    }

    @Test
    void wiresMultipleProvidersOfDifferentTypes() {
        contextRunner
                .withPropertyValues(
                        "virtualization.providers.production.type=proxmox",
                        "virtualization.providers.production.endpoint=https://pve.example.com:8006",
                        "virtualization.providers.production.token-id=root@pam!sdk",
                        "virtualization.providers.production.token-secret=s3cr3t",
                        "virtualization.providers.containers.type=incus",
                        "virtualization.providers.containers.endpoint=https://incus.example.com:8443",
                        "virtualization.providers.containers.client-cert-path=" + certPath(),
                        "virtualization.providers.containers.client-key-path=" + keyPath())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Map<String, ?> providers = context.getBean(ProviderRegistry.class).all();
                    assertThat(providers).containsOnlyKeys("production", "containers");
                });
    }

    @Test
    void missingRequiredFieldFailsStartupWithUnderstandableMessage() {
        contextRunner
                .withPropertyValues(
                        "virtualization.providers.production.type=proxmox",
                        "virtualization.providers.production.endpoint=https://pve.example.com:8006",
                        "virtualization.providers.production.token-id=root@pam!sdk")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessage("Provider 'production' has type 'proxmox' but 'token-secret' is missing.");
                });
    }

    @Test
    void userDefinedProviderRegistryBeanIsNotOverridden() {
        contextRunner.withUserConfiguration(CustomRegistryConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ProviderRegistry.class);
            assertThat(context.getBean(ProviderRegistry.class)).isSameAs(CustomRegistryConfig.CUSTOM_REGISTRY);
        });
    }

    @Test
    void userDefinedVirtualizationClientBeanIsNotOverridden() {
        contextRunner.withUserConfiguration(CustomClientConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(VirtualizationClient.class);
            assertThat(context.getBean(VirtualizationClient.class)).isSameAs(CustomClientConfig.CUSTOM_CLIENT);
        });
    }

    private static String certPath() {
        return VirtualizationAutoConfigurationTest.class.getResource("/tls/client-cert.pem").getPath();
    }

    private static String keyPath() {
        return VirtualizationAutoConfigurationTest.class.getResource("/tls/client-key.pem").getPath();
    }

    @Configuration
    static class CustomRegistryConfig {
        static final ProviderRegistry CUSTOM_REGISTRY = new ProviderRegistry(Map.of());

        @Bean
        ProviderRegistry providerRegistry() {
            return CUSTOM_REGISTRY;
        }
    }

    @Configuration
    static class CustomImageRegistryConfig {
        static final ImageProviderRegistry CUSTOM_REGISTRY = new ImageProviderRegistry(Map.of());

        @Bean
        ImageProviderRegistry imageProviderRegistry() {
            return CUSTOM_REGISTRY;
        }
    }

    @Configuration
    static class CustomClientConfig {
        static final VirtualizationClient CUSTOM_CLIENT = new VirtualizationClient(new ProviderRegistry(Map.of()));

        @Bean
        VirtualizationClient virtualizationClient() {
            return CUSTOM_CLIENT;
        }
    }
}
