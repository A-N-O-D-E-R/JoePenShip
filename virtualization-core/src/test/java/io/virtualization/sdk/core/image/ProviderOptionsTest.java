package io.virtualization.sdk.core.image;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderOptionsTest {

    @Test
    void emptyHasNoValues() {
        assertThat(ProviderOptions.empty().asMap()).isEmpty();
        assertThat(ProviderOptions.empty().get("profile")).isEmpty();
    }

    @Test
    void ofExposesGivenValues() {
        ProviderOptions options = ProviderOptions.of(Map.of("profile", "gpu", "priority", 5));

        assertThat(options.getString("profile")).contains("gpu");
        assertThat(options.get("priority")).contains(5);
        assertThat(options.get("missing")).isEmpty();
    }

    @Test
    void asMapIsImmutable() {
        ProviderOptions options = ProviderOptions.of(Map.of("k", "v"));

        assertThat(options.asMap()).isUnmodifiable();
    }
}
