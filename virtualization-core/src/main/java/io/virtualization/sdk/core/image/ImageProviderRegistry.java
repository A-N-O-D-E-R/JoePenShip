package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * An immutable, instance-based registry of named {@link ImageProvider}s. Mirrors {@code
 * io.virtualization.sdk.core.ProviderRegistry} — the same pattern, for the image side of the SDK.
 */
public final class ImageProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(ImageProviderRegistry.class);

    private final Map<String, ImageProvider> providers;

    public ImageProviderRegistry(Map<String, ImageProvider> providers) {
        Objects.requireNonNull(providers, "providers must not be null");
        for (Map.Entry<String, ImageProvider> entry : providers.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("provider name must not be blank");
            }
            Objects.requireNonNull(entry.getValue(), "provider must not be null for name '" + entry.getKey() + "'");
        }
        this.providers = Map.copyOf(providers);
        log.debug("Initialized image provider registry with {} provider(s): {}", this.providers.size(), this.providers.keySet());
    }

    /**
     * @throws ConfigurationException if no image provider is registered under the given name
     */
    public ImageProvider get(String name) {
        ImageProvider provider = providers.get(name);
        if (provider == null) {
            throw new ConfigurationException("No image provider registered under name '" + name + "'");
        }
        return provider;
    }

    public Map<String, ImageProvider> all() {
        return providers;
    }
}
