package io.virtualization.sdk.spring;

import io.virtualization.sdk.acme.mock.Dns01AcmeProvider;
import io.virtualization.sdk.certificate.AcmeProvider;
import io.virtualization.sdk.certificate.AcmeProviderRegistry;
import io.virtualization.sdk.certificate.CertificateManager;
import io.virtualization.sdk.certificate.CertificateRepository;
import io.virtualization.sdk.certificate.CertificateStore;
import io.virtualization.sdk.certificate.DefaultCertificateManager;
import io.virtualization.sdk.certificate.InMemoryCertificateRepository;
import io.virtualization.sdk.certificate.InMemoryCertificateStore;
import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.dns.DnsProviderRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wires a {@link CertificateManager} on top of an {@link AcmeProviderRegistry} built from {@code
 * virtualization.certificates.providers.*} — active only when {@code
 * virtualization.certificates.enabled=true}. Only {@code type: mock} exists (a {@link
 * Dns01AcmeProvider} composing the named entry from {@link DnsAutoConfiguration}'s {@link
 * DnsProviderRegistry} plus this class's {@link CertificateStore} bean) — see {@link
 * CertificateProperties}.
 *
 * <p>{@link InMemoryCertificateStore}/{@link InMemoryCertificateRepository} only — a deployment
 * that needs real secret storage or persistence supplies its own beans, backed off for via
 * {@code @ConditionalOnMissingBean}. Never exposes {@code CertificateMaterial} through any bean
 * consumed by the REST layer.
 */
@AutoConfiguration(after = DnsAutoConfiguration.class)
@EnableConfigurationProperties(CertificateProperties.class)
@ConditionalOnProperty(prefix = "virtualization.certificates", name = "enabled", havingValue = "true")
public class CertificateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CertificateStore certificateStore() {
        return new InMemoryCertificateStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public CertificateRepository certificateRepository() {
        return new InMemoryCertificateRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public AcmeProviderRegistry acmeProviderRegistry(CertificateProperties properties, DnsProviderRegistry dnsProviders, CertificateStore certificateStore) {
        Map<String, AcmeProvider> providers = new LinkedHashMap<>();
        properties.providers().forEach((name, entry) -> providers.put(name, createAcmeProvider(name, entry, dnsProviders, certificateStore)));
        return new AcmeProviderRegistry(providers);
    }

    @Bean
    @ConditionalOnMissingBean
    public CertificateManager certificateManager(CertificateRepository repository, AcmeProviderRegistry acmeProviders) {
        return new DefaultCertificateManager(repository, acmeProviders);
    }

    private static AcmeProvider createAcmeProvider(
            String name, CertificateProperties.ProviderEntry entry, DnsProviderRegistry dnsProviders, CertificateStore certificateStore) {
        if (entry.type() == null || entry.type().isBlank()) {
            throw new ConfigurationException("ACME provider '" + name + "' is missing required field 'type'.");
        }
        if (!"mock".equals(entry.type())) {
            throw new ConfigurationException("ACME provider '" + name + "' has unknown type '" + entry.type() + "'");
        }
        if (entry.dnsProvider() == null || entry.dnsProvider().isBlank()) {
            throw new ConfigurationException("ACME provider '" + name + "' is missing required field 'dns-provider'.");
        }
        return new Dns01AcmeProvider(dnsProviders.get(entry.dnsProvider()), certificateStore);
    }
}
