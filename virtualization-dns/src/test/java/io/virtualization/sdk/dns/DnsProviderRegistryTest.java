package io.virtualization.sdk.dns;

import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.dns.support.FakeDnsProvider;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DnsProviderRegistryTest {

    private final DnsProvider provider = FakeDnsProvider.named("cloudflare");

    @Test
    void getReturnsRegisteredProvider() {
        DnsProviderRegistry registry = new DnsProviderRegistry(Map.of("cloudflare", provider));

        assertThat(registry.get("cloudflare")).isSameAs(provider);
    }

    @Test
    void getThrowsConfigurationExceptionForUnknownName() {
        DnsProviderRegistry registry = new DnsProviderRegistry(Map.of("cloudflare", provider));

        assertThatThrownBy(() -> registry.get("route53")).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void allIsUnmodifiableAndComplete() {
        DnsProviderRegistry registry = new DnsProviderRegistry(Map.of("cloudflare", provider));

        assertThat(registry.all()).containsExactly(Map.entry("cloudflare", provider));
        assertThat(registry.all()).isUnmodifiable();
    }

    @Test
    void rejectsBlankProviderName() {
        Map<String, DnsProvider> providers = new HashMap<>();
        providers.put(" ", provider);

        assertThatIllegalArgumentException().isThrownBy(() -> new DnsProviderRegistry(providers));
    }

    @Test
    void rejectsNullProvider() {
        Map<String, DnsProvider> providers = new HashMap<>();
        providers.put("cloudflare", null);

        assertThatNullPointerException().isThrownBy(() -> new DnsProviderRegistry(providers));
    }

    @Test
    void rejectsNullMap() {
        assertThatNullPointerException().isThrownBy(() -> new DnsProviderRegistry(null));
    }
}
