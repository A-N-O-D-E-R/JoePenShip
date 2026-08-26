package io.virtualization.sdk.dns;

import io.virtualization.sdk.core.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * An immutable, instance-based registry of named {@link DnsProvider}s (e.g. {@code "cloudflare" ->
 * cloudflareProvider}). Mirrors {@code io.virtualization.sdk.core.ProviderRegistry}/{@code
 * ImageProviderRegistry} — the same pattern, for the DNS side of the SDK.
 */
public final class DnsProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(DnsProviderRegistry.class);

    private final Map<String, DnsProvider> providers;

    public DnsProviderRegistry(Map<String, DnsProvider> providers) {
        Objects.requireNonNull(providers, "providers must not be null");
        for (Map.Entry<String, DnsProvider> entry : providers.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("provider name must not be blank");
            }
            Objects.requireNonNull(entry.getValue(), "provider must not be null for name '" + entry.getKey() + "'");
        }
        this.providers = Map.copyOf(providers);
        log.debug("Initialized DNS provider registry with {} provider(s): {}", this.providers.size(), this.providers.keySet());
    }

    /**
     * @throws ConfigurationException if no DNS provider is registered under the given name
     */
    public DnsProvider get(String name) {
        DnsProvider provider = providers.get(name);
        if (provider == null) {
            throw new ConfigurationException("No DNS provider registered under name '" + name + "'");
        }
        return provider;
    }

    public Map<String, DnsProvider> all() {
        return providers;
    }
}
