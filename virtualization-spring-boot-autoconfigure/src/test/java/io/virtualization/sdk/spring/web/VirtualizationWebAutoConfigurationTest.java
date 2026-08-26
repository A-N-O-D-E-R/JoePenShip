package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.ProviderRegistry;
import io.virtualization.sdk.core.image.ImageProviderRegistry;
import io.virtualization.sdk.spring.CertificateAutoConfiguration;
import io.virtualization.sdk.spring.DnsAutoConfiguration;
import io.virtualization.sdk.spring.DomainAutoConfiguration;
import io.virtualization.sdk.spring.VirtualizationAutoConfiguration;
import io.virtualization.sdk.spring.VpsAutoConfiguration;
import io.virtualization.sdk.spring.web.support.FakeImageProvider;
import io.virtualization.sdk.spring.web.support.FakeVirtualizationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualizationWebAutoConfigurationTest {

    private final AutoConfigurations autoConfigurations =
            AutoConfigurations.of(VirtualizationAutoConfiguration.class, VirtualizationWebAutoConfiguration.class);

    @Test
    void controllersAreNotRegisteredInANonWebApplication() {
        new ApplicationContextRunner().withConfiguration(autoConfigurations).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ImageController.class);
            assertThat(context).doesNotHaveBean(WorkloadController.class);
        });
    }

    @Test
    void controllersAreRegisteredInAServletWebApplication() {
        new WebApplicationContextRunner().withConfiguration(autoConfigurations).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ImageController.class);
            assertThat(context).hasSingleBean(WorkloadController.class);
            assertThat(context).hasSingleBean(RestExceptionHandler.class);
        });
    }

    @Test
    void webEnabledFalseDisablesTheControllers() {
        new WebApplicationContextRunner()
                .withConfiguration(autoConfigurations)
                .withPropertyValues("virtualization.web.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ImageController.class);
                });
    }

    @Test
    void vpsControllerRegistersOnlyWhenVpsProviderConfigured() {
        AutoConfigurations withVps = AutoConfigurations.of(
                VirtualizationAutoConfiguration.class, VpsAutoConfiguration.class, VirtualizationWebAutoConfiguration.class);
        WebApplicationContextRunner base = new WebApplicationContextRunner()
                .withConfiguration(withVps)
                .withBean(ProviderRegistry.class, () -> new ProviderRegistry(Map.of("fake", new FakeVirtualizationProvider())))
                .withBean(ImageProviderRegistry.class, () -> new ImageProviderRegistry(Map.of("fake", new FakeImageProvider())));

        base.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(VpsController.class);
        });

        base.withPropertyValues("virtualization.vps.provider=fake").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(VpsController.class);
        });
    }

    @Test
    void domainControllerRegistersOnlyWhenDomainsEnabled() {
        AutoConfigurations withDomains = AutoConfigurations.of(
                VirtualizationAutoConfiguration.class, DnsAutoConfiguration.class, DomainAutoConfiguration.class, VirtualizationWebAutoConfiguration.class);
        WebApplicationContextRunner base = new WebApplicationContextRunner().withConfiguration(withDomains);

        base.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DomainController.class);
        });

        base.withPropertyValues("virtualization.domains.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DomainController.class);
        });
    }

    @Test
    void certificateControllerRegistersOnlyWhenCertificatesEnabled() {
        AutoConfigurations withCertificates = AutoConfigurations.of(
                VirtualizationAutoConfiguration.class, DnsAutoConfiguration.class, CertificateAutoConfiguration.class,
                VirtualizationWebAutoConfiguration.class);
        WebApplicationContextRunner base = new WebApplicationContextRunner()
                .withConfiguration(withCertificates)
                .withPropertyValues(
                        "virtualization.dns.providers.cloudflare.type=mock",
                        "virtualization.certificates.providers.letsencrypt.type=mock",
                        "virtualization.certificates.providers.letsencrypt.dns-provider=cloudflare");

        base.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(CertificateController.class);
        });

        base.withPropertyValues("virtualization.certificates.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(CertificateController.class);
        });
    }
}
