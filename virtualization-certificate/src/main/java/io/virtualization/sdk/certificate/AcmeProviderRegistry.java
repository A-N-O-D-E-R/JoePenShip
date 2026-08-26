package io.virtualization.sdk.certificate;

import io.virtualization.sdk.core.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * An immutable, instance-based registry of named {@link AcmeProvider}s (e.g. {@code "letsencrypt"
 * -> letsEncryptProvider}). Mirrors {@code io.virtualization.sdk.core.ProviderRegistry}/{@code
 * DnsProviderRegistry} — the same pattern, for the certificate side of the SDK.
 */
public final class AcmeProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(AcmeProviderRegistry.class);

    private final Map<String, AcmeProvider> providers;

    public AcmeProviderRegistry(Map<String, AcmeProvider> providers) {
        Objects.requireNonNull(providers, "providers must not be null");
        for (Map.Entry<String, AcmeProvider> entry : providers.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("provider name must not be blank");
            }
            Objects.requireNonNull(entry.getValue(), "provider must not be null for name '" + entry.getKey() + "'");
        }
        this.providers = Map.copyOf(providers);
        log.debug("Initialized ACME provider registry with {} provider(s): {}", this.providers.size(), this.providers.keySet());
    }

    /**
     * @throws ConfigurationException if no ACME provider is registered under the given name
     */
    public AcmeProvider get(String name) {
        AcmeProvider provider = providers.get(name);
        if (provider == null) {
            throw new ConfigurationException("No ACME provider registered under name '" + name + "'");
        }
        return provider;
    }

    public Map<String, AcmeProvider> all() {
        return providers;
    }
}
