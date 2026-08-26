package io.virtualization.sdk.certificate;

import io.virtualization.sdk.certificate.support.FakeAcmeProvider;
import io.virtualization.sdk.core.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcmeProviderRegistryTest {

    private final AcmeProvider provider = FakeAcmeProvider.succeeding();

    @Test
    void getReturnsRegisteredProvider() {
        AcmeProviderRegistry registry = new AcmeProviderRegistry(Map.of("letsencrypt", provider));

        assertThat(registry.get("letsencrypt")).isSameAs(provider);
    }

    @Test
    void getThrowsConfigurationExceptionForUnknownName() {
        AcmeProviderRegistry registry = new AcmeProviderRegistry(Map.of("letsencrypt", provider));

        assertThatThrownBy(() -> registry.get("zerossl")).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void allIsUnmodifiableAndComplete() {
        AcmeProviderRegistry registry = new AcmeProviderRegistry(Map.of("letsencrypt", provider));

        assertThat(registry.all()).containsExactly(Map.entry("letsencrypt", provider));
        assertThat(registry.all()).isUnmodifiable();
    }

    @Test
    void rejectsBlankProviderName() {
        Map<String, AcmeProvider> providers = new HashMap<>();
        providers.put(" ", provider);

        assertThatIllegalArgumentException().isThrownBy(() -> new AcmeProviderRegistry(providers));
    }

    @Test
    void rejectsNullProvider() {
        Map<String, AcmeProvider> providers = new HashMap<>();
        providers.put("letsencrypt", null);

        assertThatNullPointerException().isThrownBy(() -> new AcmeProviderRegistry(providers));
    }

    @Test
    void rejectsNullMap() {
        assertThatNullPointerException().isThrownBy(() -> new AcmeProviderRegistry(null));
    }
}
