package io.virtualization.sdk.core;

import io.virtualization.sdk.core.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * An immutable, instance-based registry of named {@link VirtualizationProvider}s (e.g.
 * {@code "production" -> proxmoxProvider}).
 *
 * <p>Holds no static or global mutable state — an application can construct as many registries
 * as it needs, each independently configured.
 */
public final class ProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(ProviderRegistry.class);

    private final Map<String, VirtualizationProvider> providers;

    public ProviderRegistry(Map<String, VirtualizationProvider> providers) {
        Objects.requireNonNull(providers, "providers must not be null");
        for (Map.Entry<String, VirtualizationProvider> entry : providers.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("provider name must not be blank");
            }
            Objects.requireNonNull(entry.getValue(), "provider must not be null for name '" + entry.getKey() + "'");
        }
        this.providers = Map.copyOf(providers);
        log.debug("Initialized provider registry with {} provider(s): {}", this.providers.size(), this.providers.keySet());
    }

    /**
     * @throws ConfigurationException if no provider is registered under the given name
     */
    public VirtualizationProvider get(String name) {
        VirtualizationProvider provider = providers.get(name);
        if (provider == null) {
            throw new ConfigurationException("No provider registered under name '" + name + "'");
        }
        return provider;
    }

    public Map<String, VirtualizationProvider> all() {
        return providers;
    }
}
