package io.virtualization.sdk.backup;

import io.virtualization.sdk.core.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * An immutable, instance-based registry of named {@link BackupProvider}s. Mirrors {@code
 * io.virtualization.sdk.storage.StorageProviderRegistry} — the same pattern, for the backup side
 * of the SDK.
 */
public final class BackupProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(BackupProviderRegistry.class);

    private final Map<String, BackupProvider> providers;

    public BackupProviderRegistry(Map<String, BackupProvider> providers) {
        Objects.requireNonNull(providers, "providers must not be null");
        for (Map.Entry<String, BackupProvider> entry : providers.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("provider name must not be blank");
            }
            Objects.requireNonNull(entry.getValue(), "provider must not be null for name '" + entry.getKey() + "'");
        }
        this.providers = Map.copyOf(providers);
        log.debug("Initialized backup provider registry with {} provider(s): {}", this.providers.size(), this.providers.keySet());
    }

    /**
     * @throws ConfigurationException if no backup provider is registered under the given name
     */
    public BackupProvider get(String name) {
        BackupProvider provider = providers.get(name);
        if (provider == null) {
            throw new ConfigurationException("No backup provider registered under name '" + name + "'");
        }
        return provider;
    }

    public Map<String, BackupProvider> all() {
        return providers;
    }
}
