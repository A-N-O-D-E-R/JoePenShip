package io.virtualization.sdk.core;

import io.virtualization.sdk.core.image.ImageProvider;
import io.virtualization.sdk.core.image.ImageProviderRegistry;
import io.virtualization.sdk.core.image.support.FakeImageProvider;
import io.virtualization.sdk.core.image.ImageCapabilities;
import io.virtualization.sdk.core.support.FakeVirtualizationProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualizationClientTest {

    @Test
    void delegatesToRegistry() {
        VirtualizationProvider provider = new FakeVirtualizationProvider(ProviderCapabilities.of());
        ProviderRegistry registry = new ProviderRegistry(Map.of("production", provider));
        VirtualizationClient client = new VirtualizationClient(registry);

        assertThat(client.provider("production")).isSameAs(provider);
        assertThat(client.providers()).containsExactly(Map.entry("production", provider));
    }

    @Test
    void singleArgConstructorHasNoImageProviders() {
        VirtualizationClient client = new VirtualizationClient(new ProviderRegistry(Map.of()));

        assertThat(client.imageProviders()).isEmpty();
    }

    @Test
    void delegatesToImageRegistry() {
        ImageProvider images = new FakeImageProvider(ImageCapabilities.of());
        VirtualizationClient client = new VirtualizationClient(
                new ProviderRegistry(Map.of()), new ImageProviderRegistry(Map.of("production", images)));

        assertThat(client.images("production")).isSameAs(images);
        assertThat(client.imageProviders()).containsExactly(Map.entry("production", images));
    }
}
