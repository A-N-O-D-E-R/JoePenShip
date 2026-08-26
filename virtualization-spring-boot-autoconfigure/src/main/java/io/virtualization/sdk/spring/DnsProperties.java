package io.virtualization.sdk.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Binds:
 * <pre>{@code
 * virtualization:
 *   dns:
 *     providers:
 *       cloudflare:
 *         type: mock
 *         zones: [example.com, example.net]
 * }</pre>
 * Only {@code type: mock} exists today (backed by {@code virtualization-dns-mock}'s {@code
 * InMemoryDnsProvider}, pre-seeded with {@code zones}) — no real Cloudflare/Route53 provider is
 * implemented yet, and this module never hard-codes one; a real provider type is a later addition
 * to {@link io.virtualization.sdk.spring.DnsAutoConfiguration}, not a rewrite of this shape.
 */
@ConfigurationProperties(prefix = "virtualization.dns")
public record DnsProperties(Map<String, ProviderEntry> providers) {

    public DnsProperties {
        providers = providers != null ? Map.copyOf(providers) : Map.of();
    }

    public record ProviderEntry(String type, List<String> zones) {
        public ProviderEntry {
            zones = zones != null ? List.copyOf(zones) : List.of();
        }
    }
}
