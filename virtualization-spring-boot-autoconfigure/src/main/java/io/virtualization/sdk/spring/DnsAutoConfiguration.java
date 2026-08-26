package io.virtualization.sdk.spring;

import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.dns.DnsProvider;
import io.virtualization.sdk.dns.DnsProviderRegistry;
import io.virtualization.sdk.dns.mock.InMemoryDnsProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wires a {@link DnsProviderRegistry} from {@code virtualization.dns.providers.*} — always
 * created (possibly empty), same stance {@link VirtualizationAutoConfiguration} takes for {@code
 * ProviderRegistry}/{@code ImageProviderRegistry}. Only {@code type: mock} exists — see {@link
 * DnsProperties}.
 */
@AutoConfiguration
@EnableConfigurationProperties(DnsProperties.class)
public class DnsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DnsProviderRegistry dnsProviderRegistry(DnsProperties properties) {
        Map<String, DnsProvider> providers = new LinkedHashMap<>();
        properties.providers().forEach((name, entry) -> providers.put(name, createDnsProvider(name, entry)));
        return new DnsProviderRegistry(providers);
    }

    private static DnsProvider createDnsProvider(String name, DnsProperties.ProviderEntry entry) {
        if (entry.type() == null || entry.type().isBlank()) {
            throw new ConfigurationException("DNS provider '" + name + "' is missing required field 'type'.");
        }
        if (!"mock".equals(entry.type())) {
            throw new ConfigurationException("DNS provider '" + name + "' has unknown type '" + entry.type() + "'");
        }
        InMemoryDnsProvider provider = new InMemoryDnsProvider(name);
        entry.zones().forEach(provider::addZone);
        return provider;
    }
}
