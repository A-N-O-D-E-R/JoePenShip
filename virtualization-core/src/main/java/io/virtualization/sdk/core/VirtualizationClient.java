package io.virtualization.sdk.core;

import io.virtualization.sdk.core.image.ImageProvider;
import io.virtualization.sdk.core.image.ImageProviderRegistry;

import java.util.Map;
import java.util.Objects;

/**
 * Public entry point to the SDK: a thin, immutable wrapper over a {@link ProviderRegistry} and an
 * {@link ImageProviderRegistry} — {@link VirtualizationProvider} and {@link ImageProvider} are
 * independent, separately injectable abstractions, each with its own named registry.
 */
public final class VirtualizationClient {

    private final ProviderRegistry registry;
    private final ImageProviderRegistry imageRegistry;

    public VirtualizationClient(ProviderRegistry registry) {
        this(registry, new ImageProviderRegistry(Map.of()));
    }

    public VirtualizationClient(ProviderRegistry registry, ImageProviderRegistry imageRegistry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.imageRegistry = Objects.requireNonNull(imageRegistry, "imageRegistry must not be null");
    }

    /**
     * @throws io.virtualization.sdk.core.exception.ConfigurationException if no provider is
     *     registered under the given name
     */
    public VirtualizationProvider provider(String name) {
        return registry.get(name);
    }

    public Map<String, VirtualizationProvider> providers() {
        return registry.all();
    }

    /**
     * @throws io.virtualization.sdk.core.exception.ConfigurationException if no image provider is
     *     registered under the given name
     */
    public ImageProvider images(String name) {
        return imageRegistry.get(name);
    }

    public Map<String, ImageProvider> imageProviders() {
        return imageRegistry.all();
    }
}
