package io.virtualization.sdk.cli.config;

import com.fasterxml.jackson.databind.JsonNode;
import io.virtualization.sdk.acme.mock.Dns01AcmeProvider;
import io.virtualization.sdk.certificate.AcmeProvider;
import io.virtualization.sdk.certificate.AcmeProviderRegistry;
import io.virtualization.sdk.certificate.CertificateStore;
import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.dns.DnsProviderRegistry;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads {@code virtualization.certificates.providers.<name>} — same shape as the Spring Boot
 * starter's {@code CertificateAutoConfiguration}, minus its bean-gating flag. Only {@code
 * type: mock} is accepted, each entry composing the named {@link DnsProviderRegistry} entry with
 * the caller-supplied {@link CertificateStore}.
 */
public final class CertificateConfigLoader {

    private CertificateConfigLoader() {}

    public static AcmeProviderRegistry loadRegistry(Path configPath, DnsProviderRegistry dnsProviders, CertificateStore certificateStore) {
        Map<String, AcmeProvider> providers = new LinkedHashMap<>();
        JsonNode section = ConfigYaml.section(configPath, "virtualization", "certificates", "providers");
        section.fields().forEachRemaining(field -> {
            String name = field.getKey();
            CertificateProviderEntry entry = ConfigYaml.MAPPER.convertValue(field.getValue(), CertificateProviderEntry.class);
            providers.put(name, createAcmeProvider(name, entry, dnsProviders, certificateStore));
        });
        return new AcmeProviderRegistry(providers);
    }

    private static AcmeProvider createAcmeProvider(
            String name, CertificateProviderEntry entry, DnsProviderRegistry dnsProviders, CertificateStore certificateStore) {
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
