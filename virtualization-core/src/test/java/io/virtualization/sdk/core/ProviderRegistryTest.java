package io.virtualization.sdk.core;

import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.core.support.FakeVirtualizationProvider;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderRegistryTest {

    private final VirtualizationProvider provider = new FakeVirtualizationProvider(ProviderCapabilities.of());

    @Test
    void getReturnsRegisteredProvider() {
        ProviderRegistry registry = new ProviderRegistry(Map.of("production", provider));

        assertThat(registry.get("production")).isSameAs(provider);
    }

    @Test
    void getThrowsConfigurationExceptionForUnknownName() {
        ProviderRegistry registry = new ProviderRegistry(Map.of("production", provider));

        assertThatThrownBy(() -> registry.get("staging")).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void allIsUnmodifiableAndComplete() {
        ProviderRegistry registry = new ProviderRegistry(Map.of("production", provider));

        assertThat(registry.all()).containsExactly(Map.entry("production", provider));
        assertThat(registry.all()).isUnmodifiable();
    }

    @Test
    void rejectsBlankProviderName() {
        Map<String, VirtualizationProvider> providers = new HashMap<>();
        providers.put(" ", provider);

        assertThatIllegalArgumentException().isThrownBy(() -> new ProviderRegistry(providers));
    }

    @Test
    void rejectsNullProvider() {
        Map<String, VirtualizationProvider> providers = new HashMap<>();
        providers.put("production", null);

        assertThatNullPointerException().isThrownBy(() -> new ProviderRegistry(providers));
    }

    @Test
    void rejectsNullMap() {
        assertThatNullPointerException().isThrownBy(() -> new ProviderRegistry(null));
    }
}
