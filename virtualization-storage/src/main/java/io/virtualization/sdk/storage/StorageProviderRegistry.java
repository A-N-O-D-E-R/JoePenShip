package io.virtualization.sdk.storage;

import io.virtualization.sdk.core.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * An immutable, instance-based registry of named {@link StorageProvider}s. Mirrors {@code
 * io.virtualization.sdk.dns.DnsProviderRegistry} — the same pattern, for the storage side of the
 * SDK. Deliberately not threaded through {@code VirtualizationClient}: that class lives in
 * {@code virtualization-core}, which cannot depend on this module.
 */
public final class StorageProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(StorageProviderRegistry.class);

    private final Map<String, StorageProvider> providers;

    public StorageProviderRegistry(Map<String, StorageProvider> providers) {
        Objects.requireNonNull(providers, "providers must not be null");
        for (Map.Entry<String, StorageProvider> entry : providers.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("provider name must not be blank");
            }
            Objects.requireNonNull(entry.getValue(), "provider must not be null for name '" + entry.getKey() + "'");
        }
        this.providers = Map.copyOf(providers);
        log.debug("Initialized storage provider registry with {} provider(s): {}", this.providers.size(), this.providers.keySet());
    }

    /**
     * @throws ConfigurationException if no storage provider is registered under the given name
     */
    public StorageProvider get(String name) {
        StorageProvider provider = providers.get(name);
        if (provider == null) {
            throw new ConfigurationException("No storage provider registered under name '" + name + "'");
        }
        return provider;
    }

    public Map<String, StorageProvider> all() {
        return providers;
    }
}
